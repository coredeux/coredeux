package com.coredeux.core.redis.service.impl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis-backed implementation of {@link CoredeuxDataAccessService}.
 */
public class DefaultCoredeuxRedisDataAccessService implements CoredeuxDataAccessService {

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<SearchParams>> SEARCH_PARAMS_LIST = new TypeReference<>() {
    };

    private final RedisCommands<String, String> commands;
    private final ConcurrentMap<Class<?>, Field> identifierFieldCache = new ConcurrentHashMap<>();
    private final String keyPrefix;

    public DefaultCoredeuxRedisDataAccessService(StatefulRedisConnection<String, String> connection,
            String keyPrefix) {
        Objects.requireNonNull(connection, "connection");
        this.commands = connection.sync();
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix.trim();
    }

    @Override
    public <T> T load(String id, Class<T> type) {
        validateLoadInput(id, type);
        try {
            String payload = commands.get(redisKey(type, id));
            return payload == null ? null : OBJECT_MAPPER.readValue(payload, type);
        } catch (Exception exception) {
            throw wrap("Unable to load entity of type " + typeName(type) + " for identifier '" + id + "'", exception);
        }
    }

    @Override
    public <T> String save(T entity) {
        validateEntity(entity, "save");
        try {
            Field idField = identifierField(entity.getClass());
            Object identifier = extractIdentifier(entity);
            if (identifier == null || (identifier instanceof String stringIdentifier && stringIdentifier.isBlank())) {
                identifier = generateIdentifier(entity.getClass(), idField);
                setIdentifier(entity, identifier);
            }
            String key = redisKey(entity.getClass(), String.valueOf(identifier));
            commands.set(key, toJsonString(entity));
            return String.valueOf(identifier);
        } catch (RuntimeException exception) {
            throw wrap("Unable to save entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> void update(T entity) {
        validateEntity(entity, "update");
        try {
            Object identifier = requireIdentifier(entity, "update");
            commands.set(redisKey(entity.getClass(), String.valueOf(identifier)), toJsonString(entity));
        } catch (RuntimeException exception) {
            throw wrap("Unable to update entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> void remove(T entity) {
        validateEntity(entity, "remove");
        try {
            Object identifier = requireIdentifier(entity, "remove");
            commands.del(redisKey(entity.getClass(), String.valueOf(identifier)));
        } catch (RuntimeException exception) {
            throw wrap("Unable to remove entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        validateSearchType(type);
        try {
            validateSearchParams(params);
            List<T> allEntities = readAllEntities(type);
            long totalResults = allEntities.size();
            List<T> filtered = filterEntities(allEntities, params);
            List<T> paged = page(filtered, pageSize, currentPage);

            return SearchResult.<T>builder()
                    .results(defaultResults(paged))
                    .pagination(buildPagination(totalResults, filtered.size(), pageSize, currentPage))
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
            RedisQueryRequest request = parseQueryRequest(resolvedQuery);
            List<SearchParams> filters = request.filters() == null ? List.of() : request.filters();
            validateSearchParams(filters);
            List<T> allEntities = readAllEntities(type);
            List<T> filtered = filterEntities(allEntities, filters);
            List<T> paged = page(filtered, pageSize, currentPage);

            return SearchResult.<T>builder()
                    .results(defaultResults(paged))
                    .pagination(buildPagination(allEntities.size(), filtered.size(), pageSize, currentPage))
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

    protected String redisNamespace(Class<?> type) {
        String typeName = type.getSimpleName();
        return keyPrefix.isBlank() ? typeName : keyPrefix + ":" + typeName;
    }

    protected String redisKey(Class<?> type, String id) {
        return redisNamespace(type) + ":" + id;
    }

    protected String redisSequenceKey(Class<?> type) {
        return redisNamespace(type) + ":seq";
    }

    protected <T> List<T> readAllEntities(Class<T> type) {
        List<String> keys = new ArrayList<>(commands.keys(redisNamespace(type) + ":*"));
        keys.sort(Comparator.naturalOrder());
        List<T> entities = new ArrayList<>(keys.size());
        for (String key : keys) {
            String payload = commands.get(key);
            if (payload == null) {
                continue;
            }
            entities.add(readEntity(payload, type));
        }
        return entities;
    }

    protected <T> T readEntity(String payload, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(payload, type);
        } catch (JsonProcessingException exception) {
            throw new CoredeuxDataAccessException("Unable to deserialize Redis payload for type " + type.getName(),
                    exception);
        }
    }

    protected <T> List<T> filterEntities(List<T> entities, List<SearchParams> params) {
        if (CollectionUtils.isEmpty(params)) {
            return entities;
        }
        List<T> filtered = new ArrayList<>();
        for (T entity : entities) {
            if (matches(entity, params)) {
                filtered.add(entity);
            }
        }
        return filtered;
    }

    protected <T> boolean matches(T entity, List<SearchParams> params) {
        for (SearchParams searchParams : params) {
            if (searchParams == null) {
                continue;
            }
            if (!matches(entity, searchParams)) {
                return false;
            }
        }
        return true;
    }

    protected void validateSearchParams(List<SearchParams> params) {
        if (CollectionUtils.isEmpty(params)) {
            return;
        }
        for (SearchParams searchParams : params) {
            if (searchParams == null) {
                continue;
            }
            normalizeRequired(searchParams.getField(), "Search field must not be blank");
            String comparator = normalizeRequired(searchParams.getComparator(), "Search comparator must not be blank")
                    .toUpperCase(Locale.ROOT);
            if (!SUPPORTED_COMPARATORS.contains(comparator)) {
                throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
            }
        }
    }

    protected <T> boolean matches(T entity, SearchParams searchParams) {
        String field = normalizeRequired(searchParams.getField(), "Search field must not be blank");
        String comparator = normalizeRequired(searchParams.getComparator(), "Search comparator must not be blank")
                .toUpperCase(Locale.ROOT);
        Object value = searchParams.getValue();

        if (!SUPPORTED_COMPARATORS.contains(comparator)) {
            throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        }

        Object fieldValue = resolveFieldValue(entity, field);
        return switch (comparator) {
            case "EQUALS" -> equalsValue(fieldValue, value);
            case "NOTEQUALS" -> !equalsValue(fieldValue, value);
            case "STARTSWITH" -> value != null && fieldValue != null && String.valueOf(fieldValue).startsWith(String.valueOf(value));
            case "ANYWHERECS" -> value != null && fieldValue != null && String.valueOf(fieldValue).contains(String.valueOf(value));
            case "ANYWHERE" -> value != null && fieldValue != null && String.valueOf(fieldValue).toLowerCase(Locale.ROOT)
                    .contains(String.valueOf(value).toLowerCase(Locale.ROOT));
            case "LESSTHANOREQUAL" -> compare(fieldValue, value) <= 0;
            case "LESSTHAN" -> compare(fieldValue, value) < 0;
            case "GREATERTHANOREQUAL" -> compare(fieldValue, value) >= 0;
            case "GREATERTHAN" -> compare(fieldValue, value) > 0;
            case "ISNULL" -> fieldValue == null;
            case "ISNOTNULL" -> fieldValue != null;
            case "ISEMPTY" -> isEmpty(fieldValue);
            case "ISNOTEMPTY" -> !isEmpty(fieldValue);
            case "CONTAINS" -> contains(fieldValue, value);
            case "NOTCONTAINS" -> !contains(fieldValue, value);
            default -> throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        };
    }

    protected Object resolveFieldValue(Object entity, String fieldPath) {
        Object current = entity;
        for (String segment : fieldPath.split("\\.")) {
            if (current == null) {
                return null;
            }
            Field field = ReflectionUtils.findField(current.getClass(), segment);
            if (field == null) {
                throw new CoredeuxValidationException(
                        "Unable to resolve field '" + fieldPath + "' on class: " + current.getClass().getName());
            }
            ReflectionUtils.makeAccessible(field);
            current = ReflectionUtils.getField(field, current);
        }
        return current;
    }

    protected boolean equalsValue(Object fieldValue, Object value) {
        if (fieldValue == null || value == null) {
            return fieldValue == null && value == null;
        }
        if (fieldValue instanceof Number left && value instanceof Number right) {
            return compareNumbers(left, right) == 0;
        }
        return Objects.equals(String.valueOf(fieldValue), String.valueOf(value));
    }

    protected int compare(Object fieldValue, Object value) {
        if (fieldValue == null || value == null) {
            throw new CoredeuxValidationException("Comparator values must not be null");
        }
        if (fieldValue instanceof Number left && value instanceof Number right) {
            return compareNumbers(left, right);
        }
        if (fieldValue instanceof Comparable<?> comparable) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Comparable<Object> left = (Comparable) comparable;
            return left.compareTo(value);
        }
        if (value instanceof Comparable<?> comparable) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Comparable<Object> right = (Comparable) comparable;
            return -right.compareTo(fieldValue);
        }
        return String.valueOf(fieldValue).compareTo(String.valueOf(value));
    }

    protected int compareNumbers(Number left, Number right) {
        BigDecimal leftValue = new BigDecimal(String.valueOf(left));
        BigDecimal rightValue = new BigDecimal(String.valueOf(right));
        return leftValue.compareTo(rightValue);
    }

    protected boolean contains(Object fieldValue, Object value) {
        if (fieldValue == null || value == null) {
            return false;
        }
        if (fieldValue instanceof Collection<?> collection) {
            if (value instanceof Collection<?> values) {
                return collection.containsAll(values);
            }
            return collection.contains(value);
        }
        if (fieldValue.getClass().isArray()) {
            Object[] array = (Object[]) fieldValue;
            if (value instanceof Collection<?> values) {
                return values.stream().allMatch(item -> contains(array, item));
            }
            return contains(array, value);
        }
        return String.valueOf(fieldValue).contains(String.valueOf(value));
    }

    protected boolean contains(Object[] array, Object value) {
        for (Object item : array) {
            if (Objects.equals(item, value)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isEmpty(Object fieldValue) {
        if (fieldValue == null) {
            return true;
        }
        if (fieldValue instanceof String string) {
            return string.isBlank();
        }
        if (fieldValue instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (fieldValue instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        if (fieldValue.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(fieldValue) == 0;
        }
        return false;
    }

    protected String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new CoredeuxValidationException(message);
        }
        return value.trim();
    }

    protected <T> List<T> page(List<T> results, int pageSize, int currentPage) {
        if (!isPagingEnabled(pageSize, currentPage) || CollectionUtils.isEmpty(results)) {
            return results;
        }
        int fromIndex = Math.min(Math.max(currentPage - 1, 0) * pageSize, results.size());
        int toIndex = Math.min(fromIndex + pageSize, results.size());
        return results.subList(fromIndex, toIndex);
    }

    protected boolean isPagingEnabled(int pageSize, int currentPage) {
        return pageSize > 0 && currentPage > 0;
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

    protected <T> List<T> defaultResults(List<T> results) {
        return CollectionUtils.isEmpty(results) ? List.of() : results;
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
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new CoredeuxValidationException("Unable to serialize query parameter value", exception);
        }
    }

    protected String toJsonString(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new CoredeuxValidationException("Unable to serialize Redis entity", exception);
        }
    }

    protected RedisQueryRequest parseQueryRequest(String query) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(query);
            if (node.isArray()) {
                return new RedisQueryRequest(OBJECT_MAPPER.convertValue(node, SEARCH_PARAMS_LIST));
            }
            if (node.isObject()) {
                if (node.has("filters")) {
                    return new RedisQueryRequest(OBJECT_MAPPER.convertValue(node.get("filters"), SEARCH_PARAMS_LIST));
                }
                if (node.has("field") || node.has("comparator")) {
                    return new RedisQueryRequest(List.of(OBJECT_MAPPER.convertValue(node, SearchParams.class)));
                }
            }
            throw new CoredeuxValidationException(
                    "Redis query must be a JSON array of filters or an object containing filters");
        } catch (JsonProcessingException exception) {
            throw new CoredeuxValidationException("Unable to parse Redis query", exception);
        }
    }

    protected Object generateIdentifier(Class<?> type, Field idField) {
        Class<?> targetType = idField.getType();
        if (targetType == String.class) {
            return UUID.randomUUID().toString();
        }
        if (targetType == UUID.class) {
            return UUID.randomUUID();
        }
        if (targetType == Long.class || targetType == long.class) {
            return commands.incr(redisSequenceKey(type));
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Math.toIntExact(commands.incr(redisSequenceKey(type)));
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.valueOf(Long.toString(commands.incr(redisSequenceKey(type))));
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.valueOf(Long.toString(commands.incr(redisSequenceKey(type))));
        }
        if (targetType == BigInteger.class) {
            return BigInteger.valueOf(commands.incr(redisSequenceKey(type)));
        }
        if (targetType == BigDecimal.class) {
            return BigDecimal.valueOf(commands.incr(redisSequenceKey(type)));
        }
        return UUID.randomUUID().toString();
    }

    protected Object extractIdentifier(Object entity) {
        Field field = identifierField(entity.getClass());
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, entity);
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
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, entity, convertIdentifier(String.valueOf(identifier), field.getType()));
    }

    protected Field identifierField(Class<?> type) {
        return identifierFieldCache.computeIfAbsent(type, this::findIdentifierField);
    }

    protected Field findIdentifierField(Class<?> type) {
        Field annotated = findAnnotatedField(type, "org.springframework.data.annotation.Id");
        if (annotated != null) {
            return annotated;
        }
        Field fallback = ReflectionUtils.findField(type, "id");
        if (fallback == null) {
            throw new CoredeuxValidationException("Unable to resolve identifier field for class: " + type.getName());
        }
        return fallback;
    }

    protected Field findAnnotatedField(Class<?> type, String annotationClassName) {
        final Field[] found = new Field[1];
        ReflectionUtils.doWithFields(type, field -> {
            if (fieldHasAnnotation(field, annotationClassName)) {
                found[0] = field;
            }
        });
        return found[0];
    }

    protected Object convertIdentifier(String value, Class<?> targetType) {
        if (targetType == null || targetType == String.class || targetType.isAssignableFrom(String.class)) {
            return value;
        }
        if (targetType == UUID.class) {
            return UUID.fromString(value);
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
        if (targetType.isEnum()) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) targetType, value);
            return enumValue;
        }
        Object factoryValue = invokeStringFactory(targetType, value);
        if (factoryValue != null) {
            return factoryValue;
        }
        return value;
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

    protected CoredeuxDataAccessException wrap(String message, Exception exception) {
        if (exception instanceof CoredeuxDataAccessException dataAccessException) {
            return dataAccessException;
        }
        if (exception instanceof CoredeuxValidationException validationException) {
            throw validationException;
        }
        return new CoredeuxDataAccessException(message, exception);
    }

    protected record RedisQueryRequest(List<SearchParams> filters) {
    }

    private boolean fieldHasAnnotation(Field field, String annotationClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends java.lang.annotation.Annotation> annotationType =
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName(annotationClassName);
            return field.isAnnotationPresent(annotationType);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    protected static final class BeanUtils {
        private BeanUtils() {
        }

        static void copyProperties(Object source, Object target) {
            if (source == null || target == null) {
                return;
            }
            ReflectionUtils.doWithFields(source.getClass(), field -> {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    return;
                }
                Field targetField = ReflectionUtils.findField(target.getClass(), field.getName());
                if (targetField == null) {
                    return;
                }
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.makeAccessible(targetField);
                Object value = ReflectionUtils.getField(field, source);
                ReflectionUtils.setField(targetField, target, value);
            });
        }
    }

    protected static final class CollectionUtils {
        private CollectionUtils() {
        }

        static boolean isEmpty(java.util.Collection<?> collection) {
            return collection == null || collection.isEmpty();
        }

        static boolean isEmpty(java.util.Map<?, ?> map) {
            return map == null || map.isEmpty();
        }
    }

    protected static final class ReflectionUtils {
        private ReflectionUtils() {
        }

        static void makeAccessible(Field field) {
            if (field != null) {
                field.setAccessible(true);
            }
        }

        static Object getField(Field field, Object target) {
            try {
                return field.get(target);
            } catch (IllegalAccessException exception) {
                throw new CoredeuxDataAccessException("Unable to read field " + field.getName(), exception);
            }
        }

        static void setField(Field field, Object target, Object value) {
            try {
                field.set(target, value);
            } catch (IllegalAccessException exception) {
                throw new CoredeuxDataAccessException("Unable to write field " + field.getName(), exception);
            }
        }

        static Field findField(Class<?> type, String name) {
            Class<?> current = type;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.getName().equals(name)) {
                        return field;
                    }
                }
                current = current.getSuperclass();
            }
            return null;
        }

        static void doWithFields(Class<?> type, java.util.function.Consumer<Field> consumer) {
            Class<?> current = type;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    consumer.accept(field);
                }
                current = current.getSuperclass();
            }
        }
    }
}
