package com.coredeux.core.elasticsearch.service.impl;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;

/**
 * Elasticsearch-backed implementation of {@link CoredeuxDataAccessService}
 * using the official Elasticsearch Java client.
 */
@SuppressWarnings("java:S3011")
public class DefaultCoredeuxElasticsearchDataAccessService implements CoredeuxDataAccessService {

    private static final Set<String> SUPPORTED_COMPARATORS = Set.of(
            "EQUALS",
            "NOTEQUALS",
            "STARTSWITH",
            "ANYWHERECS",
            "ANYWHERE",
            "LESSTHANOREQUAL",
            "LESSTHAN",
            "GREATERTHANOREQUAL",
            "GREATERTHAN",
            "ISNULL",
            "ISNOTNULL",
            "ISEMPTY",
            "ISNOTEMPTY",
            "CONTAINS",
            "NOTCONTAINS");

    private final ElasticsearchGateway gateway;
    private final ConcurrentMap<Class<?>, Field> identifierFieldCache = new ConcurrentHashMap<>();
    private final String defaultIndexPrefix;

    public DefaultCoredeuxElasticsearchDataAccessService(ElasticsearchGateway gateway, String defaultIndexPrefix) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.defaultIndexPrefix = defaultIndexPrefix == null ? "" : defaultIndexPrefix.trim();
    }

    public DefaultCoredeuxElasticsearchDataAccessService(ElasticsearchClient client, String defaultIndexPrefix) {
        this(new NativeElasticsearchGateway(client), defaultIndexPrefix);
    }

    public DefaultCoredeuxElasticsearchDataAccessService(ElasticsearchClient client) {
        this(client, "");
    }

    @Override
    public <T> T load(String id, Class<T> type) {
        validateLoadInput(id, type);
        try {
            return gateway.get(indexName(type), id, type);
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entity of type " + typeName(type) + " for identifier '" + id + "'", exception);
        }
    }

    @Override
    public <T> String save(T entity) {
        validateEntity(entity, "save");
        try {
            Object identifier = extractIdentifier(entity);
            String resolvedId = Objects.requireNonNull(
                    gateway.index(indexName(entity.getClass()), identifier == null ? null : String.valueOf(identifier),
                            entity),
                    "Elasticsearch index response id must not be null");
            setIdentifier(entity, resolvedId);
            return resolvedId;
        } catch (RuntimeException exception) {
            throw wrap("Unable to save entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> void update(T entity) {
        validateEntity(entity, "update");
        try {
            Object identifier = requireIdentifier(entity, "update");
            String resolvedId = Objects.requireNonNull(
                    gateway.index(indexName(entity.getClass()), String.valueOf(identifier), entity),
                    "Elasticsearch index response id must not be null");
            setIdentifier(entity, resolvedId);
        } catch (RuntimeException exception) {
            throw wrap("Unable to update entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> void remove(T entity) {
        validateEntity(entity, "remove");
        try {
            Object identifier = requireIdentifier(entity, "remove");
            gateway.delete(indexName(entity.getClass()), String.valueOf(identifier));
        } catch (RuntimeException exception) {
            throw wrap("Unable to remove entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        validateSearchType(type);
        try {
            String indexName = indexName(type);
            Query structuredQuery = buildStructuredQuery(params);
            long totalResults = gateway.count(indexName, QueryBuilders.matchAll(matchAll -> matchAll));
            long filteredResults = gateway.count(indexName, structuredQuery);
            List<T> results = gateway.search(indexName, structuredQuery, type, pageSize, currentPage);

            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, filteredResults, pageSize, currentPage))
                    .build();
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entities for type " + typeName(type), exception);
        }
    }

    @Override
    public Set<String> supportedComparators(Class<?> type) {
        return SUPPORTED_COMPARATORS;
    }

    @Override
    public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage) {
        validateQueryInput(query, type);
        try {
            String resolvedQuery = resolveQueryTemplate(query, params);
            Query queryObject = resolveQuery(resolvedQuery);
            String indexName = indexName(type);
            long totalResults = gateway.count(indexName, queryObject);
            List<T> results = gateway.search(indexName, queryObject, type, pageSize, currentPage);

            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, totalResults, pageSize, currentPage))
                    .build();
        } catch (RuntimeException exception) {
            throw wrap("Unable to execute query for type " + typeName(type), exception);
        }
    }

    @Override
    public <T> void refresh(T entity) {
        validateEntity(entity, "refresh");
        try {
            Object identifier = requireIdentifier(entity, "refresh");
            @SuppressWarnings("unchecked")
            T refreshed = (T) load(String.valueOf(identifier), entity.getClass());
            if (refreshed != null) {
                copyFields(refreshed, entity);
            }
        } catch (RuntimeException exception) {
            throw wrap("Unable to refresh entity of type " + entity.getClass().getName(), exception);
        }
    }

    protected void validateLoadInput(String id, Class<?> type) {
        if (id == null || id.isBlank()) {
            throw new CoredeuxValidationException("Load identifier must not be blank");
        }
        validateSearchType(type);
    }

    protected void validateQueryInput(String query, Class<?> type) {
        if (query == null || query.isBlank()) {
            throw new CoredeuxValidationException("Query string must not be blank");
        }
        validateSearchType(type);
    }

    protected void validateSearchType(Class<?> type) {
        if (type == null) {
            throw new CoredeuxValidationException("Entity type must not be null");
        }
    }

    protected <T> void validateEntity(T entity, String action) {
        if (entity == null) {
            throw new CoredeuxValidationException("Entity must not be null for " + action);
        }
    }

    protected String indexName(Class<?> type) {
        String indexName = type.getSimpleName();
        if (!defaultIndexPrefix.isBlank()) {
            return defaultIndexPrefix + "-" + indexName;
        }
        return indexName;
    }

    protected Query buildStructuredQuery(List<SearchParams> params) {
        if (params == null || params.isEmpty()) {
            return QueryBuilders.matchAll(matchAll -> matchAll);
        }

        List<Query> clauses = new ArrayList<>();
        for (SearchParams searchParams : params) {
            if (searchParams == null) {
                continue;
            }
            Query clause = buildCriteria(searchParams);
            if (clause != null) {
                clauses.add(clause);
            }
        }

        if (clauses.isEmpty()) {
            return QueryBuilders.matchAll(matchAll -> matchAll);
        }
        return QueryBuilders.bool(bool -> bool.must(clauses));
    }

    protected Query buildCriteria(SearchParams searchParams) {
        String field = normalizeRequired(searchParams.getField(), "Search field must not be blank");
        String comparator = normalizeRequired(searchParams.getComparator(), "Search comparator must not be blank")
                .toUpperCase(Locale.ROOT);
        Object value = searchParams.getValue();

        if (!SUPPORTED_COMPARATORS.contains(comparator)) {
            throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        }

        return switch (comparator) {
            case "EQUALS" -> value != null ? termQuery(field, value) : null;
            case "NOTEQUALS" -> value != null ? QueryBuilders.bool(bool -> bool.mustNot(termQuery(field, value))) : null;
            case "STARTSWITH" -> value != null ? QueryBuilders.prefix(prefix -> prefix.field(field).value(String.valueOf(value))) : null;
            case "ANYWHERECS" -> value != null ? QueryBuilders.wildcard(wildcard -> wildcard.field(field).value("*" + value + "*")) : null;
            case "ANYWHERE" -> value != null ? QueryBuilders.matchPhrase(matchPhrase -> matchPhrase.field(field).query(String.valueOf(value))) : null;
            case "LESSTHANOREQUAL" -> value != null ? rangeQuery(field, null, null, null, value) : null;
            case "LESSTHAN" -> value != null ? rangeQuery(field, null, null, value, null) : null;
            case "GREATERTHANOREQUAL" -> value != null ? rangeQuery(field, value, null, null, null) : null;
            case "GREATERTHAN" -> value != null ? rangeQuery(field, null, value, null, null) : null;
            case "ISNULL" -> QueryBuilders.bool(bool -> bool.mustNot(QueryBuilders.exists(exists -> exists.field(field))));
            case "ISNOTNULL" -> QueryBuilders.exists(exists -> exists.field(field));
            case "ISEMPTY" -> QueryBuilders.bool(bool -> bool.mustNot(QueryBuilders.exists(exists -> exists.field(field))));
            case "ISNOTEMPTY" -> QueryBuilders.exists(exists -> exists.field(field));
            case "CONTAINS" -> value != null ? containsQuery(field, value) : null;
            case "NOTCONTAINS" -> value != null ? notContainsQuery(field, value) : null;
            default -> throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        };
    }

    protected Query termQuery(String field, Object value) {
        if (value instanceof Boolean booleanValue) {
            return QueryBuilders.term(term -> term.field(field).value(booleanValue));
        }
        if (value instanceof Integer integerValue) {
            return QueryBuilders.term(term -> term.field(field).value(integerValue.longValue()));
        }
        if (value instanceof Long longValue) {
            return QueryBuilders.term(term -> term.field(field).value(longValue));
        }
        if (value instanceof Short shortValue) {
            return QueryBuilders.term(term -> term.field(field).value(shortValue.longValue()));
        }
        if (value instanceof Byte byteValue) {
            return QueryBuilders.term(term -> term.field(field).value(byteValue.longValue()));
        }
        if (value instanceof Float floatValue) {
            return QueryBuilders.term(term -> term.field(field).value(floatValue.doubleValue()));
        }
        if (value instanceof Double doubleValue) {
            return QueryBuilders.term(term -> term.field(field).value(doubleValue));
        }
        return QueryBuilders.term(term -> term.field(field).value(String.valueOf(value)));
    }

    protected Query rangeQuery(String field, Object gt, Object gte, Object lt, Object lte) {
        return QueryBuilders.range(range -> {
            range.field(field);
            if (gt != null) {
                range.gt(JsonData.of(gt));
            }
            if (gte != null) {
                range.gte(JsonData.of(gte));
            }
            if (lt != null) {
                range.lt(JsonData.of(lt));
            }
            if (lte != null) {
                range.lte(JsonData.of(lte));
            }
            return range;
        });
    }

    protected Query containsQuery(String field, Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<Query> clauses = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null) {
                    clauses.add(termQuery(field, item));
                }
            }
            if (clauses.isEmpty()) {
                return null;
            }
            return QueryBuilders.bool(bool -> bool.should(clauses).minimumShouldMatch("1"));
        }
        return QueryBuilders.wildcard(wildcard -> wildcard.field(field).value("*" + value + "*"));
    }

    protected Query notContainsQuery(String field, Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<Query> clauses = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null) {
                    clauses.add(termQuery(field, item));
                }
            }
            if (clauses.isEmpty()) {
                return null;
            }
            return QueryBuilders.bool(bool -> bool.mustNot(clauses));
        }
        return QueryBuilders.bool(bool -> bool.mustNot(QueryBuilders.wildcard(wildcard -> wildcard.field(field).value("*" + value + "*"))));
    }

    protected Query resolveQuery(String resolvedQuery) {
        try {
            String querySource = extractQuerySource(resolvedQuery);
            return Query.of(queryBuilder -> queryBuilder.withJson(new StringReader(querySource)));
        } catch (Exception exception) {
            throw new CoredeuxValidationException("Unable to parse Elasticsearch query", exception);
        }
    }

    protected String extractQuerySource(String resolvedQuery) {
        String trimmed = resolvedQuery == null ? "" : resolvedQuery.trim();
        if (!trimmed.startsWith("{") || !trimmed.contains("\"query\"")) {
            return trimmed;
        }

        int queryKeyIndex = trimmed.indexOf("\"query\"");
        int colonIndex = trimmed.indexOf(':', queryKeyIndex);
        if (colonIndex < 0) {
            return trimmed;
        }

        int startIndex = colonIndex + 1;
        while (startIndex < trimmed.length() && Character.isWhitespace(trimmed.charAt(startIndex))) {
            startIndex++;
        }
        if (startIndex >= trimmed.length()) {
            return trimmed;
        }

        char opening = trimmed.charAt(startIndex);
        if (opening != '{' && opening != '[') {
            return trimmed;
        }
        return extractJsonValue(trimmed, startIndex);
    }

    protected String extractJsonValue(String json, int startIndex) {
        char opening = json.charAt(startIndex);
        char closing = opening == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = startIndex; index < json.length(); index++) {
            char current = json.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == opening) {
                depth++;
            } else if (current == closing) {
                depth--;
                if (depth == 0) {
                    return json.substring(startIndex, index + 1);
                }
            }
        }
        return json.substring(startIndex);
    }

    protected PaginationData buildPagination(long totalResults, long resultSize, int pageSize, int currentPage) {
        PaginationData paginationData = new PaginationData();
        paginationData.setCurrentPage((long) currentPage);
        paginationData.setPageSize((long) pageSize);
        if (isPagingEnabled(pageSize, currentPage)) {
            paginationData.setTotalResults(totalResults);
            paginationData.setResultSize(resultSize);
            paginationData.setTotalPages(resultSize == 0 ? 0L : (long) Math.ceil(resultSize / (double) pageSize));
        }
        return paginationData;
    }

    protected boolean isPagingEnabled(int pageSize, int currentPage) {
        return pageSize > 0 && currentPage > 0;
    }

    protected <T> List<T> defaultResults(List<T> results) {
        return results == null || results.isEmpty() ? List.of() : results;
    }

    protected String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new CoredeuxValidationException(message);
        }
        return value.trim();
    }

    protected String typeName(Class<?> type) {
        return type == null ? "null" : type.getName();
    }

    protected Object extractIdentifier(Object entity) {
        Field field = identifierField(entity.getClass());
        makeAccessible(field);
        return readField(field, entity);
    }

    protected Object requireIdentifier(Object entity, String action) {
        Object identifier = extractIdentifier(entity);
        if (identifier == null || (identifier instanceof String stringIdentifier && stringIdentifier.isBlank())) {
            throw new CoredeuxValidationException("Entity identifier must not be blank for " + action);
        }
        return identifier;
    }

    protected void setIdentifier(Object entity, Object identifier) {
        Field field = identifierField(entity.getClass());
        makeAccessible(field);
        writeField(field, entity, convertIdentifier(String.valueOf(identifier), field.getType()));
    }

    protected Field identifierField(Class<?> type) {
        return identifierFieldCache.computeIfAbsent(type, this::findIdentifierField);
    }

    protected Field findIdentifierField(Class<?> type) {
        Field annotated = findAnnotatedField(type,
                "org.springframework.data.annotation.Id",
                "jakarta.persistence.Id",
                "javax.persistence.Id");
        if (annotated != null) {
            return annotated;
        }
        Field fallback = findField(type, "id");
        if (fallback == null) {
            throw new CoredeuxValidationException("Unable to resolve identifier field for class: " + type.getName());
        }
        return fallback;
    }

    protected Field findAnnotatedField(Class<?> type, String... annotationClassNames) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                for (String annotationClassName : annotationClassNames) {
                    if (hasAnnotation(field, annotationClassName)) {
                        return field;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    protected boolean hasAnnotation(Field field, String annotationClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends java.lang.annotation.Annotation> annotationType =
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName(annotationClassName);
            return field.isAnnotationPresent(annotationType);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    protected Field findField(Class<?> type, String fieldName) {
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

    protected void copyFields(Object source, Object target) {
        Class<?> current = source.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Field targetField = findField(target.getClass(), field.getName());
                if (targetField == null || Modifier.isStatic(targetField.getModifiers())) {
                    continue;
                }
                makeAccessible(field);
                makeAccessible(targetField);
                writeField(targetField, target, readField(field, source));
            }
            current = current.getSuperclass();
        }
    }

    protected void makeAccessible(Field field) {
        if (field != null) {
            field.setAccessible(true);
        }
    }

    protected Object readField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new CoredeuxValidationException("Unable to read field: " + field.getName(), exception);
        }
    }

    protected void writeField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException exception) {
            throw new CoredeuxValidationException("Unable to write field: " + field.getName(), exception);
        }
    }

    protected Object convertIdentifier(String value, Class<?> targetType) {
        if (targetType == null || targetType == String.class || targetType.isAssignableFrom(String.class)) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(value);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(value);
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.valueOf(value);
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.valueOf(value);
        }
        if (targetType == BigInteger.class) {
            return new BigInteger(value);
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(value);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value);
        }
        if (targetType == UUID.class) {
            return UUID.fromString(value);
        }
        if (targetType.isEnum()) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) targetType, value);
            return enumValue;
        }
        Object factoryValue = invokeStringFactory(targetType, value);
        if (factoryValue != null) {
            return factoryValue;
        }
        throw new CoredeuxValidationException("Unsupported identifier type: " + targetType.getName());
    }

    protected Object invokeStringFactory(Class<?> targetType, String value) {
        for (String methodName : List.of("valueOf", "of", "fromString")) {
            try {
                return targetType.getMethod(methodName, String.class).invoke(null, value);
            } catch (ReflectiveOperationException exception) {
                // try next factory
            }
        }
        try {
            return targetType.getConstructor(String.class).newInstance(value);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    protected String resolveQueryTemplate(String query, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return query;
        }

        String resolved = query;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", toJsonLiteral(entry.getValue()));
        }
        if (resolved.contains("{{")) {
            throw new CoredeuxValidationException("Query template contains unresolved parameters: " + resolved);
        }
        return resolved;
    }

    protected String toJsonLiteral(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escapeJson(stringValue) + "\"";
        }
        if (value instanceof Character characterValue) {
            return "\"" + escapeJson(String.valueOf(characterValue)) + "\"";
        }
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append("\"")
                        .append(escapeJson(String.valueOf(entry.getKey())))
                        .append("\":")
                        .append(toJsonLiteral(entry.getValue()));
            }
            return builder.append('}').toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toJsonLiteral(item));
            }
            return builder.append(']').toString();
        }
        if (value.getClass().isArray()) {
            StringBuilder builder = new StringBuilder("[");
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                if (index > 0) {
                    builder.append(',');
                }
                builder.append(toJsonLiteral(Array.get(value, index)));
            }
            return builder.append(']').toString();
        }
        return "\"" + escapeJson(String.valueOf(value)) + "\"";
    }

    protected String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    protected CoredeuxDataAccessException wrap(String message, RuntimeException exception) {
        if (exception instanceof CoredeuxDataAccessException dataAccessException) {
            return dataAccessException;
        }
        if (exception instanceof CoredeuxValidationException validationException) {
            throw validationException;
        }
        return new CoredeuxDataAccessException(message, exception);
    }
}
