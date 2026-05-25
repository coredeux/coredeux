package com.coredeux.core.jdbc.service.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.impl.AbstractCoredeuxDataAccessService;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JDBC-based implementation of {@link CoredeuxDataAccessService}.
 */
public class DefaultCoredeuxJdbcDataAccessService extends AbstractCoredeuxDataAccessService {

    private static final Pattern NAMED_PARAMETER_PATTERN = Pattern.compile(":([A-Za-z_][A-Za-z0-9_]*)");

    private final DataSource dataSource;
    private final ConcurrentMap<Class<?>, JdbcEntityMetadata> metadataCache = new ConcurrentHashMap<>();
    private final String defaultSchema;

    public DefaultCoredeuxJdbcDataAccessService(DataSource dataSource, String defaultSchema) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.defaultSchema = defaultSchema == null ? "" : defaultSchema.trim();
    }

    public DefaultCoredeuxJdbcDataAccessService(Object namedParameterJdbcTemplate, String defaultSchema) {
        this(extractDataSource(namedParameterJdbcTemplate), defaultSchema);
    }

    public DefaultCoredeuxJdbcDataAccessService(java.sql.Connection connection, String defaultSchema) {
        this(new ConnectionBackedDataSource(connection), defaultSchema);
    }

    @Override
    public <T> T load(String id, Class<T> type) {
        validateLoadInput(id, type);
        try {
            JdbcEntityMetadata metadata = metadata(type);
        String sql = "select " + selectList(metadata) + " from " + metadata.qualifiedTableName()
                + " where " + metadata.idColumn() + " = ?";
            List<T> results = query(sql, List.of(convertIdentifier(id, metadata.idField().getType())), type);
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entity of type " + typeName(type) + " for identifier '" + id + "'", exception);
        }
    }

    @Override
    public <T> String save(T entity) {
        validateEntity(entity, "save");
        try {
            JdbcEntityMetadata metadata = metadata(entity.getClass());
            Map<String, Object> values = extractColumnValues(entity, metadata, true);
            Object identifier = values.get(metadata.idColumn());

            if (identifier == null || (identifier instanceof String stringIdentifier && stringIdentifier.isBlank())) {
                String sql = "insert into " + metadata.qualifiedTableName() + " (" + insertColumns(metadata, false)
                        + ") values (" + insertPlaceholdersSql(metadata, false) + ")";
                Object generated = insertAndReturnKey(sql, values, metadata, false);
                if (generated != null) {
                    writeField(metadata.idField(), entity, convertValue(generated, metadata.idField().getType()));
                }
                return generated == null ? null : String.valueOf(generated);
            }

            String sql = "insert into " + metadata.qualifiedTableName() + " (" + insertColumns(metadata, true)
                    + ") values (" + insertPlaceholdersSql(metadata, true) + ")";
            update(sql, orderedParameters(values, metadata, true));
            return String.valueOf(identifier);
        } catch (RuntimeException exception) {
            throw wrap("Unable to save entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> void update(T entity) {
        validateEntity(entity, "update");
        try {
            JdbcEntityMetadata metadata = metadata(entity.getClass());
            Object identifier = requireIdentifier(entity, "update");
            Map<String, Object> values = extractColumnValues(entity, metadata, true);
            List<Object> parameters = orderedParameters(values, metadata, false);
            parameters.add(identifier);

            String sql = "update " + metadata.qualifiedTableName() + " set " + updateAssignmentsSql(metadata)
                    + " where " + metadata.idColumn() + " = ?";
            update(sql, parameters);
        } catch (RuntimeException exception) {
            throw wrap("Unable to update entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> void remove(T entity) {
        validateEntity(entity, "remove");
        try {
            JdbcEntityMetadata metadata = metadata(entity.getClass());
            Object identifier = requireIdentifier(entity, "remove");
            String sql = "delete from " + metadata.qualifiedTableName() + " where " + metadata.idColumn() + " = ?";
            update(sql, List.of(identifier));
        } catch (RuntimeException exception) {
            throw wrap("Unable to remove entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        validateSearchType(type);
        try {
            JdbcEntityMetadata metadata = metadata(type);
            SearchQuerySpec querySpec = buildSearchQuery(params, metadata);
            long totalResults = countAll(metadata);
            long filteredResults = countFiltered(metadata, querySpec);

            String sql = "select " + selectList(metadata) + " from " + metadata.qualifiedTableName()
                    + querySpec.whereClause()
                    + orderByClause(metadata)
                    + pagingClauseSql(pageSize, currentPage);
            List<Object> parameters = new ArrayList<>(querySpec.parameters().getValues().values());
            addPagingParameters(parameters, pageSize, currentPage);
            List<T> results = query(sql, parameters, type);

            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, filteredResults, pageSize, currentPage))
                    .build();
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entities for type " + typeName(type), exception);
        }
    }

    @Override
    public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage) {
        validateQueryInput(query, type);
        try {
            String normalizedQuery = normalizeQuery(query);
            BoundSql countSql = prepareSql("select count(*) from (" + normalizedQuery + ") coredeux_jdbc_count", params);
            long totalResults = queryForLong(countSql.sql(), countSql.parameters());

            String dataSql = normalizedQuery + pagingClauseSql(pageSize, currentPage);
            BoundSql dataBoundSql = prepareSql(dataSql, params);
            List<Object> parameters = new ArrayList<>(dataBoundSql.parameters());
            addPagingParameters(parameters, pageSize, currentPage);
            List<T> results = query(dataBoundSql.sql(), parameters, type);

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
            JdbcEntityMetadata metadata = metadata(entity.getClass());
            Object identifier = requireIdentifier(entity, "refresh");
            Object refreshed = load(String.valueOf(identifier), entity.getClass());
            if (refreshed != null) {
                copyFields(refreshed, entity);
            }
        } catch (RuntimeException exception) {
            throw wrap("Unable to refresh entity of type " + entity.getClass().getName(), exception);
        }
    }

    protected JdbcEntityMetadata metadata(Class<?> type) {
        return metadataCache.computeIfAbsent(type, this::buildMetadata);
    }

    protected JdbcEntityMetadata buildMetadata(Class<?> type) {
        Field idField = identifierField(type);
        String tableName = resolveTableName(type);
        List<JdbcFieldMapping> mappings = new ArrayList<>();
        doWithFields(type, field -> {
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
        return new JdbcEntityMetadata(type, tableName, idField, resolveColumnName(idField), List.copyOf(mappings));
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
        return insertColumns(metadata, true);
    }

    protected String insertColumns(JdbcEntityMetadata metadata, boolean includeIdentifier) {
        StringJoiner joiner = new StringJoiner(", ");
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
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

    protected String insertPlaceholders(JdbcEntityMetadata metadata, boolean includeIdentifier) {
        StringJoiner joiner = new StringJoiner(", ");
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
            joiner.add("?");
        }
        return joiner.toString();
    }

    protected String insertPlaceholdersSql(JdbcEntityMetadata metadata, boolean includeIdentifier) {
        return insertPlaceholders(metadata, includeIdentifier);
    }

    protected String orderByClause(JdbcEntityMetadata metadata) {
        return metadata.hasIdentifier() ? " order by " + metadata.idColumn() : "";
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

    protected String updateAssignmentsSql(JdbcEntityMetadata metadata) {
        StringJoiner joiner = new StringJoiner(", ");
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (mapping.isIdentifier(metadata)) {
                continue;
            }
            joiner.add(mapping.columnName() + " = ?");
        }
        return joiner.toString();
    }

    protected Map<String, Object> extractColumnValues(Object entity, JdbcEntityMetadata metadata, boolean includeIdentifier) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
            values.put(mapping.columnName(), readField(mapping.field(), entity));
        }
        return values;
    }

    protected BoundParameters mapParameters(Map<String, Object> values, JdbcEntityMetadata metadata,
            boolean includeIdentifier) {
        BoundParameters params = new BoundParameters();
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
            params.addValue(mapping.columnName(), values.get(mapping.columnName()));
        }
        return params;
    }

    protected BoundParameters mapParameters(Map<String, Object> values) {
        BoundParameters params = new BoundParameters();
        if (values != null) {
            values.forEach(params::addValue);
        }
        return params;
    }

    protected List<Object> orderedParameters(Map<String, Object> values, JdbcEntityMetadata metadata,
            boolean includeIdentifier) {
        List<Object> params = new ArrayList<>();
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
            params.add(values.get(mapping.columnName()));
        }
        return params;
    }

    protected long countAll(JdbcEntityMetadata metadata) {
        return queryForLong("select count(*) from " + metadata.qualifiedTableName(), Collections.emptyMap());
    }

    protected long countFiltered(JdbcEntityMetadata metadata, SearchQuerySpec querySpec) {
        return queryForLong("select count(*) from " + metadata.qualifiedTableName() + querySpec.whereClause(),
                new ArrayList<>(querySpec.parameters().getValues().values()));
    }

    protected SearchQuerySpec buildSearchQuery(List<SearchParams> params, JdbcEntityMetadata metadata) {
        if (params == null || params.isEmpty()) {
            return new SearchQuerySpec("", new BoundParameters());
        }

        List<String> clauses = new ArrayList<>();
        BoundParameters parameters = new BoundParameters();
        int index = 0;
        for (SearchParams searchParams : params) {
            if (searchParams == null) {
                continue;
            }
            String field = normalizeRequired(searchParams.getField(), "Search field must not be blank");
            String comparator = normalizeRequired(searchParams.getComparator(), "Search comparator must not be blank")
                    .toUpperCase(Locale.ROOT);
            String column = metadata.columnNameForField(field);
            if (!supportedComparators(metadata.type()).contains(comparator)) {
                throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
            }

            Object value = searchParams.getValue();
            String paramName = "p" + index++;
            switch (comparator) {
                case "EQUALS" -> {
                    if (value != null) {
                        clauses.add(column + " = ?");
                        parameters.addValue(paramName, value);
                    }
                }
                case "NOTEQUALS" -> {
                    if (value != null) {
                        clauses.add(column + " <> ?");
                        parameters.addValue(paramName, value);
                    }
                }
                case "STARTSWITH" -> {
                    if (value != null) {
                        clauses.add(textExpression(column) + " like ?");
                        parameters.addValue(paramName, value + "%");
                    }
                }
                case "ANYWHERECS" -> {
                    if (value != null) {
                        clauses.add(textExpression(column) + " like ?");
                        parameters.addValue(paramName, "%" + value + "%");
                    }
                }
                case "ANYWHERE" -> {
                    if (value != null) {
                        clauses.add("lower(" + textExpression(column) + ") like ?");
                        parameters.addValue(paramName, ("%" + value + "%").toLowerCase(Locale.ROOT));
                    }
                }
                case "LESSTHANOREQUAL" -> {
                    if (value != null) {
                        clauses.add(column + " <= ?");
                        parameters.addValue(paramName, value);
                    }
                }
                case "LESSTHAN" -> {
                    if (value != null) {
                        clauses.add(column + " < ?");
                        parameters.addValue(paramName, value);
                    }
                }
                case "GREATERTHANOREQUAL" -> {
                    if (value != null) {
                        clauses.add(column + " >= ?");
                        parameters.addValue(paramName, value);
                    }
                }
                case "GREATERTHAN" -> {
                    if (value != null) {
                        clauses.add(column + " > ?");
                        parameters.addValue(paramName, value);
                    }
                }
                case "ISNULL" -> clauses.add(column + " is null");
                case "ISNOTNULL" -> clauses.add(column + " is not null");
                case "ISEMPTY" -> clauses.add("coalesce(" + textExpression(column) + ", '') = ''");
                case "ISNOTEMPTY" -> clauses.add("coalesce(" + textExpression(column) + ", '') <> ''");
                case "CONTAINS" -> {
                    if (value != null) {
                        clauses.add(textExpression(column) + " like ?");
                        parameters.addValue(paramName, "%" + value + "%");
                    }
                }
                case "NOTCONTAINS" -> {
                    if (value != null) {
                        clauses.add(textExpression(column) + " not like ?");
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

    protected String pagingClauseSql(int pageSize, int currentPage) {
        return isPagingEnabled(pageSize, currentPage) ? " limit ? offset ?" : "";
    }

    protected void addPagingParameters(List<Object> params, int pageSize, int currentPage) {
        if (!isPagingEnabled(pageSize, currentPage)) {
            return;
        }
        params.add(pageSize);
        params.add(Math.max(currentPage - 1, 0) * pageSize);
    }

    protected String normalizeQuery(String query) {
        String normalized = query.trim();
        if (normalized.endsWith(";")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    protected long queryForLong(String sql, Map<String, Object> params) {
        BoundSql boundSql = prepareSql(sql, params);
        return queryForLong(boundSql.sql(), boundSql.parameters());
    }

    protected long queryForLong(String sql, List<Object> parameters) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return 0L;
                }
                Object value = resultSet.getObject(1);
                return value == null ? 0L : Long.parseLong(String.valueOf(value));
            }
        } catch (SQLException exception) {
            throw new CoredeuxDataAccessException("Unable to execute count query", exception);
        }
    }

    protected <T> List<T> query(String sql, List<Object> parameters, Class<T> type) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(readRow(resultSet, type));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new CoredeuxDataAccessException("Unable to execute query for type " + typeName(type), exception);
        }
    }

    protected int update(String sql, List<Object> parameters) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new CoredeuxDataAccessException("Unable to execute update", exception);
        }
    }

    protected Object insertAndReturnKey(String sql, Map<String, Object> values, JdbcEntityMetadata metadata,
            boolean includeIdentifier) {
        List<Object> parameters = new ArrayList<>();
        for (JdbcFieldMapping mapping : metadata.fields()) {
            if (!includeIdentifier && mapping.isIdentifier(metadata)) {
                continue;
            }
            parameters.add(values.get(mapping.columnName()));
        }
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, parameters);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getObject(1);
                }
                return null;
            }
        } catch (SQLException exception) {
            throw new CoredeuxDataAccessException("Unable to insert entity and read generated key", exception);
        }
    }

    protected void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        if (parameters == null) {
            return;
        }
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    protected BoundSql prepareSql(String sql, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new BoundSql(sql, List.of());
        }

        Matcher matcher = NAMED_PARAMETER_PATTERN.matcher(sql);
        StringBuffer buffer = new StringBuffer();
        List<Object> parameters = new ArrayList<>();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!params.containsKey(key)) {
                throw new CoredeuxValidationException("Missing query parameter: " + key);
            }
            matcher.appendReplacement(buffer, "?");
            parameters.add(params.get(key));
        }
        matcher.appendTail(buffer);
        return new BoundSql(buffer.toString(), parameters);
    }

    protected <T> T readRow(ResultSet resultSet, Class<T> type) {
        try {
            T entity = type.getDeclaredConstructor().newInstance();
            ResultSetMetaData metadata = resultSet.getMetaData();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
            String column = metadata.getColumnLabel(index).toLowerCase(Locale.ROOT);
            Field field = findField(type, column);
                if (field == null) {
                    continue;
                }
                field.setAccessible(true);
                Object value = resultSet.getObject(index);
                field.set(entity, convertValue(value, field.getType()));
            }
            return entity;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException | SQLException exception) {
            throw new CoredeuxDataAccessException("Unable to materialize entity of type " + type.getName(), exception);
        }
    }

    protected record BoundSql(String sql, List<Object> parameters) {
    }

    protected static final class BoundParameters {
        private final Map<String, Object> values = new LinkedHashMap<>();

        BoundParameters addValue(String name, Object value) {
            values.put(name, value);
            return this;
        }

        public Map<String, Object> getValues() {
            return Collections.unmodifiableMap(values);
        }
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

    protected record SearchQuerySpec(String whereClause, BoundParameters parameters) {
    }

    protected static final class ConnectionBackedDataSource implements DataSource {
        private final Connection connection;

        ConnectionBackedDataSource(Connection connection) {
            this.connection = Objects.requireNonNull(connection, "connection");
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return connection;
        }

        @Override
        public java.io.PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return iface.cast(this);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }

    private static DataSource extractDataSource(Object namedParameterJdbcTemplate) {
        if (namedParameterJdbcTemplate instanceof DataSource dataSource) {
            return dataSource;
        }
        if (namedParameterJdbcTemplate instanceof java.sql.Connection connection) {
            return new ConnectionBackedDataSource(connection);
        }
        try {
            Object jdbcTemplate = namedParameterJdbcTemplate.getClass().getMethod("getJdbcTemplate")
                    .invoke(namedParameterJdbcTemplate);
            if (jdbcTemplate instanceof DataSource dataSource) {
                return dataSource;
            }
            Object dataSource = jdbcTemplate.getClass().getMethod("getDataSource").invoke(jdbcTemplate);
            return (DataSource) dataSource;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Unable to extract DataSource from " + namedParameterJdbcTemplate,
                    exception);
        }
    }
}
