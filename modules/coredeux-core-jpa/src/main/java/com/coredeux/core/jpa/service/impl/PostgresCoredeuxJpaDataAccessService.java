package com.coredeux.core.jpa.service.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

import jakarta.persistence.Column;
import jakarta.persistence.Query;
import jakarta.persistence.Table;

/**
 * PostgreSQL-specific JPA implementation that adds JSONB/native-query support.
 */
@Service("postgresCoredeuxJpaDataAccessService")
public class PostgresCoredeuxJpaDataAccessService extends DefaultCoredeuxJpaDataAccessService {

    static final String JSONB_TEXT = "JSONB(TEXT)";
    static final String JSONB_NUMERIC = "JSONB(NUMERIC)";
    private static final String DEFAULT_ALIAS = "entity_alias";

    @Override
    @Transactional(readOnly = true)
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        if (!containsJsonComparators(params)) {
            return super.loadAll(params, type, pageSize, currentPage);
        }

        validateSearchType(type);
        try {
            NativeQuerySpec querySpec = buildNativeQuerySpec(params, type);
            String dataSql = "select " + DEFAULT_ALIAS + ".* from " + querySpec.tableName + " " + DEFAULT_ALIAS
                    + querySpec.whereClause;
            Query dataQuery = getEntityManager().createNativeQuery(dataSql, type);
            bindParameters(dataQuery, querySpec.parameters);
            applyPaging(dataQuery, pageSize, currentPage);
            @SuppressWarnings("unchecked")
            List<T> results = dataQuery.getResultList();
            detachResults(results);

            long totalResults = countAll(type);
            long filteredResults = countFilteredNative(querySpec);
            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, filteredResults, pageSize, currentPage))
                    .build();
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entities for type " + type.getName() + " using PostgreSQL JSONB support",
                    exception);
        }
    }

    @Override
    public Set<String> supportedComparators(Class<?> type) {
        Set<String> comparators = new LinkedHashSet<>(super.supportedComparators(type));
        comparators.add(JSONB_TEXT);
        comparators.add(JSONB_NUMERIC);
        return Set.copyOf(comparators);
    }

    boolean containsJsonComparators(List<SearchParams> params) {
        if (CollectionUtils.isEmpty(params)) {
            return false;
        }
        return params.stream()
                .filter(Objects::nonNull)
                .map(SearchParams::getComparator)
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(value -> JSONB_TEXT.equals(value) || JSONB_NUMERIC.equals(value));
    }

    <T> NativeQuerySpec buildNativeQuerySpec(List<SearchParams> params, Class<T> type) {
        String tableName = resolveTableName(type);
        if (CollectionUtils.isEmpty(params)) {
            return new NativeQuerySpec(tableName, "", List.of());
        }

        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        for (SearchParams param : params) {
            if (param == null) {
                continue;
            }
            String comparator = normalizeRequired(param.getComparator(), "Search comparator must not be blank")
                    .toUpperCase(Locale.ROOT);
            if (JSONB_TEXT.equals(comparator)) {
                conditions.add(buildJsonTextCondition(type, param, parameters));
            } else if (JSONB_NUMERIC.equals(comparator)) {
                conditions.add(buildJsonNumericCondition(type, param, parameters));
            } else {
                conditions.add(buildNativeStandardCondition(type, param, parameters));
            }
        }

        String whereClause = conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
        return new NativeQuerySpec(tableName, whereClause, List.copyOf(parameters));
    }

    String buildJsonTextExpression(Class<?> type, String field) {
        String normalizedField = normalizeRequired(field, "Search field must not be blank");
        if (normalizedField.contains("->") || normalizedField.contains("#>")) {
            int operatorIndex = findOperatorIndex(normalizedField);
            String column = normalizedField.substring(0, operatorIndex).trim();
            return DEFAULT_ALIAS + "." + resolveColumnName(type, column) + normalizedField.substring(operatorIndex);
        }

        String[] parts = normalizedField.split("\\.");
        String columnName = resolveColumnName(type, parts[0]);
        if (parts.length == 1) {
            return DEFAULT_ALIAS + "." + columnName + "::text";
        }

        StringJoiner joiner = new StringJoiner(",");
        for (int index = 1; index < parts.length; index++) {
            joiner.add(parts[index].trim());
        }
        return DEFAULT_ALIAS + "." + columnName + " #>> '{" + joiner + "}'";
    }

    String buildJsonTextCondition(Class<?> type, SearchParams param, List<Object> parameters) {
        String expression = buildJsonTextExpression(type, param.getField());
        Object value = param.getValue();
        if (value instanceof Map<?, ?> map) {
            String operator = normalizeRequired(String.valueOf(map.get("operator")), "JSONB text operator must not be blank")
                    .toUpperCase(Locale.ROOT);
            Object expected = map.get("value");
            return switch (operator) {
                case EQUALS -> addParameterizedCondition(expression + " = ?", expected, parameters);
                case NOTEQUALS -> addParameterizedCondition(expression + " <> ?", expected, parameters);
                case STARTSWITH -> addParameterizedCondition(expression + " like ?", expected + "%", parameters);
                case ANYWHERECS -> addParameterizedCondition(expression + " like ?", "%" + expected + "%", parameters);
                case ANYWHERE -> addParameterizedCondition("lower(" + expression + ") like ?",
                        ("%" + expected + "%").toLowerCase(Locale.ROOT), parameters);
                default -> throw new CoredeuxValidationException("Unsupported JSONB text operator: " + operator);
            };
        }
        if (value instanceof String rawCondition && !rawCondition.isBlank()) {
            return expression + " " + rawCondition.trim();
        }
        throw new CoredeuxValidationException("JSONB text comparator requires either a raw SQL fragment string or a map value");
    }

    String buildJsonNumericCondition(Class<?> type, SearchParams param, List<Object> parameters) {
        String expression = buildJsonTextExpression(type, param.getField());
        Object value = param.getValue();
        if (value instanceof Map<?, ?> map) {
            String operator = normalizeRequired(String.valueOf(map.get("operator")), "JSONB numeric operator must not be blank")
                    .toUpperCase(Locale.ROOT);
            Object expected = map.get("value");
            String numericExpression = "cast(" + expression + " as numeric)";
            return switch (operator) {
                case EQUALS -> addParameterizedCondition(numericExpression + " = ?", expected, parameters);
                case NOTEQUALS -> addParameterizedCondition(numericExpression + " <> ?", expected, parameters);
                case LESSTHAN -> addParameterizedCondition(numericExpression + " < ?", expected, parameters);
                case LESSTHANOREQUAL -> addParameterizedCondition(numericExpression + " <= ?", expected, parameters);
                case GREATERTHAN -> addParameterizedCondition(numericExpression + " > ?", expected, parameters);
                case GREATERTHANOREQUAL -> addParameterizedCondition(numericExpression + " >= ?", expected, parameters);
                default -> throw new CoredeuxValidationException("Unsupported JSONB numeric operator: " + operator);
            };
        }
        if (value instanceof String template && !template.isBlank()) {
            return String.format(template, expression);
        }
        throw new CoredeuxValidationException(
                "JSONB numeric comparator requires either a raw SQL template string or a map value");
    }

    String buildNativeStandardCondition(Class<?> type, SearchParams param, List<Object> parameters) {
        String field = normalizeRequired(param.getField(), "Search field must not be blank");
        String comparator = normalizeRequired(param.getComparator(), "Search comparator must not be blank")
                .toUpperCase(Locale.ROOT);
        String columnExpression = DEFAULT_ALIAS + "." + resolveColumnName(type, field);
        Object value = param.getValue();

        return switch (comparator) {
            case EQUALS -> value != null ? addParameterizedCondition(columnExpression + " = ?", value, parameters) : "1 = 1";
            case NOTEQUALS -> value != null ? addParameterizedCondition(columnExpression + " <> ?", value, parameters) : "1 = 1";
            case STARTSWITH -> value != null ? addParameterizedCondition(columnExpression + " like ?", value + "%", parameters) : "1 = 1";
            case ANYWHERECS -> value != null ? addParameterizedCondition(columnExpression + " like ?", "%" + value + "%", parameters) : "1 = 1";
            case ANYWHERE -> value != null ? addParameterizedCondition("lower(" + columnExpression + ") like ?",
                    ("%" + value + "%").toLowerCase(Locale.ROOT), parameters) : "1 = 1";
            case LESSTHAN -> value != null ? addParameterizedCondition(columnExpression + " < ?", value, parameters) : "1 = 1";
            case LESSTHANOREQUAL -> value != null ? addParameterizedCondition(columnExpression + " <= ?", value, parameters) : "1 = 1";
            case GREATERTHAN -> value != null ? addParameterizedCondition(columnExpression + " > ?", value, parameters) : "1 = 1";
            case GREATERTHANOREQUAL -> value != null ? addParameterizedCondition(columnExpression + " >= ?", value, parameters) : "1 = 1";
            case ISNULL -> columnExpression + " is null";
            case ISNOTNULL -> columnExpression + " is not null";
            default -> throw new CoredeuxValidationException(
                    "Comparator " + comparator + " is not supported by native PostgreSQL JSONB search mode");
        };
    }

    String resolveTableName(Class<?> type) {
        Table table = type.getAnnotation(Table.class);
        if (table != null && table.name() != null && !table.name().isBlank()) {
            return table.name().trim();
        }
        return type.getSimpleName();
    }

    String resolveColumnName(Class<?> type, String fieldName) {
        Field field = findField(type, fieldName);
        if (field == null) {
            throw new CoredeuxValidationException("Unable to resolve field '" + fieldName + "' on class: " + type.getName());
        }
        Column column = field.getAnnotation(Column.class);
        if (column != null && column.name() != null && !column.name().isBlank()) {
            return column.name().trim();
        }
        return field.getName();
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private int findOperatorIndex(String fieldExpression) {
        int arrowIndex = fieldExpression.indexOf("->");
        int hashIndex = fieldExpression.indexOf("#>");
        if (arrowIndex < 0) {
            return hashIndex;
        }
        if (hashIndex < 0) {
            return arrowIndex;
        }
        return Math.min(arrowIndex, hashIndex);
    }

    private String addParameterizedCondition(String condition, Object value, List<Object> parameters) {
        parameters.add(value);
        return condition;
    }

    private void bindParameters(Query query, List<Object> parameters) {
        for (int index = 0; index < parameters.size(); index++) {
            query.setParameter(index + 1, parameters.get(index));
        }
    }

    private void applyPaging(Query query, int pageSize, int currentPage) {
        if (!isPagingEnabled(pageSize, currentPage)) {
            return;
        }
        query.setFirstResult(Math.max(currentPage - 1, 0) * pageSize);
        query.setMaxResults(pageSize);
    }

    private long countFilteredNative(NativeQuerySpec querySpec) {
        Query countQuery = getEntityManager().createNativeQuery(
                "select count(*) from " + querySpec.tableName + " " + DEFAULT_ALIAS + querySpec.whereClause);
        bindParameters(countQuery, querySpec.parameters);
        Object result = countQuery.getSingleResult();
        return ((Number) result).longValue();
    }

    static final class NativeQuerySpec {
        final String tableName;
        final String whereClause;
        final List<Object> parameters;

        NativeQuerySpec(String tableName, String whereClause, List<Object> parameters) {
            this.tableName = tableName;
            this.whereClause = whereClause;
            this.parameters = parameters;
        }
    }
}
