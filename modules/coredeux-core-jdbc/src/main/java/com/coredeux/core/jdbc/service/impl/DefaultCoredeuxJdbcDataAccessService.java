package com.coredeux.core.jdbc.service.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.StringJoiner;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JDBC-based implementation of {@link CoredeuxDataAccessService}.
 */
@Service("defaultCoredeuxJdbcDataAccessService")
public class DefaultCoredeuxJdbcDataAccessService implements CoredeuxDataAccessService {

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

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ConcurrentMap<Class<?>, JdbcEntityMetadata> metadataCache = new ConcurrentHashMap<>();
    private final String defaultSchema;

    public DefaultCoredeuxJdbcDataAccessService(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Value("${coredeux.jdbc.default-schema:}") String defaultSchema) {
        this.namedParameterJdbcTemplate = Objects.requireNonNull(namedParameterJdbcTemplate, "namedParameterJdbcTemplate");
        this.defaultSchema = defaultSchema == null ? "" : defaultSchema.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public <T> T load(String id, Class<T> type) {
        validateLoadInput(id, type);
        try {
            JdbcEntityMetadata metadata = metadata(type);
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue(metadata.idColumn(), convertIdentifier(id, metadata.idField().getType()));
            String sql = "select " + selectList(metadata) + " from " + metadata.qualifiedTableName()
                    + " where " + metadata.idColumn() + " = :" + metadata.idColumn();
            List<T> results = namedParameterJdbcTemplate.query(sql, params, rowMapper(type));
            return CollectionUtils.isEmpty(results) ? null : results.get(0);
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entity of type " + typeName(type) + " for identifier '" + id + "'", exception);
        }
    }

    @Override
    @Transactional
    public <T> String save(T entity) {
        validateEntity(entity, "save");
        try {
            JdbcEntityMetadata metadata = metadata(entity.getClass());
            Map<String, Object> values = extractColumnValues(entity, metadata, true);
            Object identifier = values.get(metadata.idColumn());

            if (identifier == null) {
                String insertSql = "insert into " + metadata.qualifiedTableName() + " (" + insertColumns(metadata)
                        + ") values (" + insertParameters(metadata, false) + ")";
                MapSqlParameterSource params = mapParameters(values, metadata, false);
                if (metadata.hasIdentifier()) {
                    SimpleJdbcInsert insert = new SimpleJdbcInsert(
                            Objects.requireNonNull(namedParameterJdbcTemplate.getJdbcTemplate().getDataSource(),
                                    "A DataSource is required for generated-key inserts"))
                            .withTableName(metadata.qualifiedTableName())
                            .usingColumns(insertColumnNames(metadata, false))
                            .usingGeneratedKeyColumns(metadata.idColumn());
                    Number generatedKey = insert.executeAndReturnKey(params);
                    if (generatedKey != null) {
                        setFieldValue(entity, metadata.idField(), convertIdentifier(generatedKey.toString(),
                                metadata.idField().getType()));
                    }
                    return generatedKey == null ? null : String.valueOf(generatedKey);
                }
                namedParameterJdbcTemplate.update(insertSql, params);
                return null;
            }

            String insertSql = "insert into " + metadata.qualifiedTableName() + " (" + insertColumns(metadata)
                    + ") values (" + insertParameters(metadata, true) + ")";
            namedParameterJdbcTemplate.update(insertSql, mapParameters(values, metadata, true));
            return String.valueOf(identifier);
        } catch (RuntimeException exception) {
            throw wrap("Unable to save entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    @Transactional
    public <T> void update(T entity) {
        validateEntity(entity, "update");
        try {
            JdbcEntityMetadata metadata = metadata(entity.getClass());
            Object identifier = requireIdentifier(entity, metadata, "update");
            Map<String, Object> values = extractColumnValues(entity, metadata, true);
            MapSqlParameterSource params = mapParameters(values, metadata, true);
            params.addValue(metadata.idColumn(), identifier);

            String sql = "update " + metadata.qualifiedTableName() + " set " + updateAssignments(metadata)
                    + " where " + metadata.idColumn() + " = :" + metadata.idColumn();
            namedParameterJdbcTemplate.update(sql, params);
        } catch (RuntimeException exception) {
            throw wrap("Unable to update entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    @Transactional
    public <T> void remove(T entity) {
        validateEntity(entity, "remove");
        try {
            JdbcEntityMetadata metadata = metadata(entity.getClass());
            Object identifier = requireIdentifier(entity, metadata, "remove");
            String sql = "delete from " + metadata.qualifiedTableName() + " where " + metadata.idColumn() + " = :"
                    + metadata.idColumn();
            namedParameterJdbcTemplate.update(sql,
                    new MapSqlParameterSource(metadata.idColumn(), identifier));
        } catch (RuntimeException exception) {
            throw wrap("Unable to remove entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        validateSearchType(type);
        try {
            JdbcEntityMetadata metadata = metadata(type);
            SearchQuerySpec querySpec = buildSearchQuery(params, metadata);
            long totalResults = countAll(metadata);
            long filteredResults = countFiltered(metadata, querySpec);

            String sql = "select " + selectList(metadata) + " from " + metadata.qualifiedTableName()
                    + querySpec.whereClause
                    + orderByClause(metadata)
                    + pagingClause(pageSize, currentPage);
            MapSqlParameterSource paramsSource = new MapSqlParameterSource(querySpec.parameters.getValues());
            addPagingParameters(paramsSource, pageSize, currentPage);
            List<T> results = namedParameterJdbcTemplate.query(sql, paramsSource, rowMapper(type));

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
    @Transactional(readOnly = true)
    public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage) {
        validateQueryInput(query, type);
        try {
            String normalizedQuery = normalizeQuery(query);
            MapSqlParameterSource paramsSource = mapParameters(params);
            long totalResults = namedParameterJdbcTemplate.queryForObject(
                    "select count(*) from (" + normalizedQuery + ") coredeux_jdbc_count", paramsSource, Long.class);

            String dataSql = normalizedQuery + pagingClause(pageSize, currentPage);
            addPagingParameters(paramsSource, pageSize, currentPage);
            List<T> results = namedParameterJdbcTemplate.query(dataSql, paramsSource, rowMapper(type));

            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, totalResults, pageSize, currentPage))
                    .build();
        } catch (RuntimeException exception) {
            throw wrap("Unable to execute query for type " + typeName(type), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public <T> void refresh(T entity) {
        validateEntity(entity, "refresh");
        try {
            JdbcEntityMetadata metadata = metadata(entity.getClass());
            Object identifier = requireIdentifier(entity, metadata, "refresh");
            Object refreshed = load(String.valueOf(identifier), entity.getClass());
            if (refreshed != null) {
                BeanUtils.copyProperties(refreshed, entity);
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

    protected String typeName(Class<?> type) {
        return type == null ? "null" : type.getName();
    }

    protected String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new CoredeuxValidationException(message);
        }
        return value.trim();
    }

    protected <T> List<T> defaultResults(List<T> results) {
        return CollectionUtils.isEmpty(results) ? List.of() : results;
    }

    protected JdbcEntityMetadata metadata(Class<?> type) {
        return metadataCache.computeIfAbsent(type, this::buildMetadata);
    }

    protected JdbcEntityMetadata buildMetadata(Class<?> type) {
        Field idField = findIdentifierField(type);
        String tableName = resolveTableName(type);
        List<JdbcFieldMapping> mappings = new ArrayList<>();
        ReflectionUtils.doWithFields(type, field -> {
            if (shouldPersist(field)) {
                mappings.add(new JdbcFieldMapping(field, resolveColumnName(field)));
            }
        });
        if (mappings.isEmpty()) {
            throw new CoredeuxValidationException("No persistent fields found for class: " + type.getName());
        }
        if (idField == null) {
            throw new CoredeuxValidationException("Unable to resolve identifier field for class: " + type.getName());
        }
        String idColumn = resolveColumnName(idField);
        return new JdbcEntityMetadata(type, tableName, idField, idColumn, List.copyOf(mappings));
    }

    protected boolean shouldPersist(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
            return false;
        }
        return !field.isAnnotationPresent(jakarta.persistence.Transient.class);
    }

    protected String resolveTableName(Class<?> type) {
        Table table = type.getAnnotation(Table.class);
        if (table != null && table.name() != null && !table.name().isBlank()) {
            return qualify(table.name().trim());
        }
        return qualify(type.getSimpleName());
    }

    protected String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && column.name() != null && !column.name().isBlank()) {
            return column.name().trim();
        }
        return field.getName();
    }

    protected String qualify(String tableName) {
        return defaultSchema.isBlank() ? tableName : defaultSchema + "." + tableName;
    }

    protected String selectList(JdbcEntityMetadata metadata) {
        StringJoiner joiner = new StringJoiner(", ");
        for (JdbcFieldMapping mapping : metadata.fields()) {
            joiner.add(mapping.columnName() + " as " + mapping.field().getName());
        }
        return joiner.toString();
    }

    protected String insertColumns(JdbcEntityMetadata metadata) {
        StringJoiner joiner = new StringJoiner(", ");
        for (JdbcFieldMapping mapping : metadata.fields()) {
            joiner.add(mapping.columnName());
        }
        return joiner.toString();
    }

    protected String insertParameters(JdbcEntityMetadata metadata, boolean includeIdentifier) {
        StringJoiner joiner = new StringJoiner(", ");
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
            joiner.add(":" + mapping.columnName());
        }
        return joiner.toString();
    }

    protected String orderByClause(JdbcEntityMetadata metadata) {
        if (!metadata.hasIdentifier()) {
            return "";
        }
        return " order by " + metadata.idColumn();
    }

    protected String[] insertColumnNames(JdbcEntityMetadata metadata, boolean includeIdentifier) {
        List<String> columns = new ArrayList<>();
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
            columns.add(mapping.columnName());
        }
        return columns.toArray(String[]::new);
    }

    protected String updateAssignments(JdbcEntityMetadata metadata) {
        StringJoiner joiner = new StringJoiner(", ");
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (mapping.isIdentifier(metadata)) {
                continue;
            }
            joiner.add(mapping.columnName() + " = :" + mapping.columnName());
        }
        return joiner.toString();
    }

    protected Map<String, Object> extractColumnValues(Object entity, JdbcEntityMetadata metadata, boolean includeIdentifier) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
            params.addValue(mapping.columnName(), extractFieldValue(entity, mapping.field()));
        }
        return params.getValues();
    }

    protected Object extractFieldValue(Object entity, Field field) {
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, entity);
    }

    protected void setFieldValue(Object entity, Field field, Object value) {
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, entity, value);
    }

    protected MapSqlParameterSource mapParameters(Map<String, Object> values, JdbcEntityMetadata metadata,
            boolean includeIdentifier) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
            params.addValue(mapping.columnName(), values.get(mapping.columnName()));
        }
        return params;
    }

    protected MapSqlParameterSource mapParameters(Map<String, Object> values) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (values != null) {
            values.forEach(params::addValue);
        }
        return params;
    }

    protected long countAll(JdbcEntityMetadata metadata) {
        String sql = "select count(*) from " + metadata.qualifiedTableName();
        Long value = namedParameterJdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return value == null ? 0L : value;
    }

    protected long countFiltered(JdbcEntityMetadata metadata, SearchQuerySpec querySpec) {
        String sql = "select count(*) from " + metadata.qualifiedTableName() + querySpec.whereClause;
        Long value = namedParameterJdbcTemplate.queryForObject(sql, querySpec.parameters, Long.class);
        return value == null ? 0L : value;
    }

    protected SearchQuerySpec buildSearchQuery(List<SearchParams> params, JdbcEntityMetadata metadata) {
        if (CollectionUtils.isEmpty(params)) {
            return new SearchQuerySpec("", new MapSqlParameterSource());
        }

        List<String> clauses = new ArrayList<>();
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        int index = 0;
        for (SearchParams searchParams : params) {
            if (searchParams == null) {
                continue;
            }
            String field = normalizeRequired(searchParams.getField(), "Search field must not be blank");
            String comparator = normalizeRequired(searchParams.getComparator(), "Search comparator must not be blank")
                    .toUpperCase(Locale.ROOT);
            String column = metadata.columnNameForField(field);
            if (!SUPPORTED_COMPARATORS.contains(comparator)) {
                throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
            }

            Object value = searchParams.getValue();
            String paramName = "p" + index++;
            switch (comparator) {
                case "EQUALS" -> {
                    if (value != null) {
                        clauses.add(column + " = :" + paramName);
                        parameters.addValue(paramName, value);
                    }
                }
                case "NOTEQUALS" -> {
                    if (value != null) {
                        clauses.add(column + " <> :" + paramName);
                        parameters.addValue(paramName, value);
                    }
                }
                case "STARTSWITH" -> {
                    if (value != null) {
                        clauses.add(textExpression(column) + " like :" + paramName);
                        parameters.addValue(paramName, value + "%");
                    }
                }
                case "ANYWHERECS" -> {
                    if (value != null) {
                        clauses.add(textExpression(column) + " like :" + paramName);
                        parameters.addValue(paramName, "%" + value + "%");
                    }
                }
                case "ANYWHERE" -> {
                    if (value != null) {
                        clauses.add("lower(" + textExpression(column) + ") like :" + paramName);
                        parameters.addValue(paramName, ("%" + value + "%").toLowerCase(Locale.ROOT));
                    }
                }
                case "LESSTHANOREQUAL" -> {
                    if (value != null) {
                        clauses.add(column + " <= :" + paramName);
                        parameters.addValue(paramName, value);
                    }
                }
                case "LESSTHAN" -> {
                    if (value != null) {
                        clauses.add(column + " < :" + paramName);
                        parameters.addValue(paramName, value);
                    }
                }
                case "GREATERTHANOREQUAL" -> {
                    if (value != null) {
                        clauses.add(column + " >= :" + paramName);
                        parameters.addValue(paramName, value);
                    }
                }
                case "GREATERTHAN" -> {
                    if (value != null) {
                        clauses.add(column + " > :" + paramName);
                        parameters.addValue(paramName, value);
                    }
                }
                case "ISNULL" -> clauses.add(column + " is null");
                case "ISNOTNULL" -> clauses.add(column + " is not null");
                case "ISEMPTY" -> clauses.add("coalesce(" + textExpression(column) + ", '') = ''");
                case "ISNOTEMPTY" -> clauses.add("coalesce(" + textExpression(column) + ", '') <> ''");
                case "CONTAINS" -> {
                    if (value != null) {
                        clauses.add(textExpression(column) + " like :" + paramName);
                        parameters.addValue(paramName, "%" + value + "%");
                    }
                }
                case "NOTCONTAINS" -> {
                    if (value != null) {
                        clauses.add(textExpression(column) + " not like :" + paramName);
                        parameters.addValue(paramName, "%" + value + "%");
                    }
                }
                default -> throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
            }
        }

        if (clauses.isEmpty()) {
            return new SearchQuerySpec("", parameters);
        }
        return new SearchQuerySpec(" where " + String.join(" and ", clauses), parameters);
    }

    protected String textExpression(String column) {
        return "cast(" + column + " as varchar)";
    }

    protected String pagingClause(int pageSize, int currentPage) {
        return isPagingEnabled(pageSize, currentPage) ? " limit :__limit offset :__offset" : "";
    }

    protected void addPagingParameters(MapSqlParameterSource params, int pageSize, int currentPage) {
        if (!isPagingEnabled(pageSize, currentPage)) {
            return;
        }
        params.addValue("__limit", pageSize);
        params.addValue("__offset", Math.max(currentPage - 1, 0) * pageSize);
    }

    protected boolean isPagingEnabled(int pageSize, int currentPage) {
        return pageSize > 0 && currentPage > 0;
    }

    protected String normalizeQuery(String query) {
        String normalized = query.trim();
        if (normalized.endsWith(";")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
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

    protected Object requireIdentifier(Object entity, JdbcEntityMetadata metadata, String action) {
        Object identifier = extractFieldValue(entity, metadata.idField());
        if (identifier == null || (identifier instanceof String stringIdentifier && stringIdentifier.isBlank())) {
            throw new CoredeuxValidationException("Entity identifier must not be blank for " + action);
        }
        return identifier;
    }

    protected Field findIdentifierField(Class<?> type) {
        Field annotatedField = findAnnotatedField(type, Id.class);
        if (annotatedField != null) {
            return annotatedField;
        }
        return ReflectionUtils.findField(type, "id");
    }

    protected Field findAnnotatedField(Class<?> type, Class<? extends java.lang.annotation.Annotation> annotationType) {
        final Field[] found = new Field[1];
        ReflectionUtils.doWithFields(type, field -> {
            if (field.isAnnotationPresent(annotationType)) {
                found[0] = field;
            }
        });
        return found[0];
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
                // continue
            }
        }
        try {
            return targetType.getConstructor(String.class).newInstance(value);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    protected <T> org.springframework.jdbc.core.RowMapper<T> rowMapper(Class<T> type) {
        return new org.springframework.jdbc.core.BeanPropertyRowMapper<>(type);
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

    protected record JdbcEntityMetadata(Class<?> type, String tableName, Field idField, String idColumn,
            List<JdbcFieldMapping> fields) {

        String qualifiedTableName() {
            return tableName;
        }

        boolean hasIdentifier() {
            return idField != null;
        }

        String columnNameForField(String fieldName) {
            for (JdbcFieldMapping mapping : fields) {
                if (mapping.field().getName().equals(fieldName)) {
                    return mapping.columnName();
                }
            }
            throw new CoredeuxValidationException("Unable to resolve field '" + fieldName + "' on class: "
                    + type.getName());
        }
    }

    protected record JdbcFieldMapping(Field field, String columnName) {

        boolean isIdentifier(JdbcEntityMetadata metadata) {
            return field.equals(metadata.idField());
        }
    }

    protected record SearchQuerySpec(String whereClause, MapSqlParameterSource parameters) {
    }
}
