package com.coredeux.core.mongodb.service.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.impl.AbstractCoredeuxDataAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.nin;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.regex;

/**
 * MongoDB-backed implementation of {@link CoredeuxDataAccessService}.
 */
public class DefaultCoredeuxMongoDataAccessService extends AbstractCoredeuxDataAccessService {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private final MongoDatabase database;
    private final ObjectMapper objectMapper;
    public DefaultCoredeuxMongoDataAccessService(MongoClient mongoClient, String databaseName) {
        this(Objects.requireNonNull(mongoClient, "mongoClient").getDatabase(databaseName), new ObjectMapper());
    }

    public DefaultCoredeuxMongoDataAccessService(MongoDatabase database) {
        this(database, new ObjectMapper());
    }

    DefaultCoredeuxMongoDataAccessService(MongoDatabase database, ObjectMapper objectMapper) {
        this.database = Objects.requireNonNull(database, "database");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public <T> T load(String id, Class<T> type) {
        validateLoadInput(id, type);
        try {
            Object convertedId = convertIdentifier(id, resolveIdentifierType(type));
            Document document = collection(type).find(filterById(type, convertedId)).first();
            return document == null ? null : fromDocument(normalizeDocumentId(document, type), type);
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entity of type " + type.getName() + " for identifier '" + id + "'", exception);
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
            Document document = toDocument(entity);
            document.put("_id", normalizeMongoId(identifier, idField.getType()));
            collection(entity.getClass()).replaceOne(filterById(entity.getClass(),
                    normalizeMongoId(identifier, idField.getType())),
                    document, new com.mongodb.client.model.ReplaceOptions().upsert(true));
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
            Document document = toDocument(entity);
            document.put("_id", normalizeMongoId(identifier, identifierField(entity.getClass()).getType()));
            UpdateResult result = collection(entity.getClass()).replaceOne(filterById(entity.getClass(),
                    normalizeMongoId(identifier, identifierField(entity.getClass()).getType())), document,
                    new com.mongodb.client.model.ReplaceOptions().upsert(false));
            if (result.getMatchedCount() == 0L) {
                throw new CoredeuxDataAccessException("No MongoDB document found for identifier " + identifier);
            }
        } catch (RuntimeException exception) {
            throw wrap("Unable to update entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> void remove(T entity) {
        validateEntity(entity, "remove");
        try {
            Object identifier = requireIdentifier(entity, "remove");
            DeleteResult result = collection(entity.getClass()).deleteOne(filterById(entity.getClass(),
                    normalizeMongoId(identifier, identifierField(entity.getClass()).getType())));
            if (result.getDeletedCount() == 0L) {
                throw new CoredeuxDataAccessException("No MongoDB document found for identifier " + identifier);
            }
        } catch (RuntimeException exception) {
            throw wrap("Unable to remove entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        validateSearchType(type);
        try {
            Bson filter = buildSearchFilter(params);
            MongoCollection<Document> collection = collection(type);
            long totalResults = collection.countDocuments();
            long filteredResults = collection.countDocuments(filter);

            List<T> results = new ArrayList<>();
            FindIterable<Document> documents = collection.find(filter);
            if (isPagingEnabled(pageSize, currentPage)) {
                documents = documents.skip(Math.max(currentPage - 1, 0) * pageSize).limit(pageSize);
            }
            for (Document document : documents) {
                results.add(fromDocument(normalizeDocumentId(document, type), type));
            }

            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, filteredResults, pageSize, currentPage))
                    .build();
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entities for type " + type.getName(), exception);
        }
    }

    @Override
    public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage) {
        validateQueryInput(query, type);
        try {
            String resolvedQuery = resolveQueryTemplate(query, params);
            Document filterDocument = Document.parse(resolvedQuery);
            MongoCollection<Document> collection = collection(type);
            long totalResults = collection.countDocuments(filterDocument);

            FindIterable<Document> documents = collection.find(filterDocument);
            if (isPagingEnabled(pageSize, currentPage)) {
                documents = documents.skip(Math.max(currentPage - 1, 0) * pageSize).limit(pageSize);
            }

            List<T> results = new ArrayList<>();
            for (Document document : documents) {
                results.add(fromDocument(normalizeDocumentId(document, type), type));
            }

            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, totalResults, pageSize, currentPage))
                    .build();
        } catch (RuntimeException exception) {
            throw wrap("Unable to execute query for type " + type.getName(), exception);
        }
    }

    @Override
    public <T> void refresh(T entity) {
        validateEntity(entity, "refresh");
        Object identifier = requireIdentifier(entity, "refresh");
        try {
            @SuppressWarnings("unchecked")
            T refreshed = (T) load(String.valueOf(identifier), entity.getClass());
            if (refreshed != null) {
                copyProperties(refreshed, entity);
            }
        } catch (RuntimeException exception) {
            throw wrap("Unable to refresh entity of type " + entity.getClass().getName(), exception);
        }
    }

    protected <T> SearchResult<T> defaultSearchResult(List<T> results, long totalResults, long filteredResults,
            int pageSize, int currentPage) {
        return SearchResult.<T>builder()
                .results(defaultResults(results))
                .pagination(buildPagination(totalResults, filteredResults, pageSize, currentPage))
                .build();
    }

    protected Bson buildSearchFilter(List<SearchParams> params) {
        if (params == null || params.isEmpty()) {
            return new Document();
        }

        List<Bson> filters = new ArrayList<>();
        for (SearchParams searchParams : params) {
            if (searchParams == null) {
                continue;
            }
            Bson criterion = buildCriteria(searchParams);
            if (criterion != null) {
                filters.add(criterion);
            }
        }

        if (filters.isEmpty()) {
            return new Document();
        }
        return filters.size() == 1 ? filters.get(0) : and(filters);
    }

    protected Bson buildCriteria(SearchParams searchParams) {
        String field = normalizeRequired(searchParams.getField(), "Search field must not be blank");
        String comparator = normalizeRequired(searchParams.getComparator(), "Search comparator must not be blank")
                .toUpperCase(Locale.ROOT);
        Object value = searchParams.getValue();

            if (!supportedComparators(Object.class).contains(comparator)) {
                throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
            }

        return switch (comparator) {
            case "EQUALS" -> value != null ? eq(resolveFieldName(field), value) : null;
            case "NOTEQUALS" -> value != null ? ne(resolveFieldName(field), value) : null;
            case "STARTSWITH" -> value != null ? regex(resolveFieldName(field), "^" + Pattern.quote(String.valueOf(value))) : null;
            case "ANYWHERECS" -> value != null ? regex(resolveFieldName(field), ".*" + Pattern.quote(String.valueOf(value)) + ".*") : null;
            case "ANYWHERE" -> value != null
                    ? regex(resolveFieldName(field), Pattern.compile(".*" + Pattern.quote(String.valueOf(value)) + ".*",
                            Pattern.CASE_INSENSITIVE))
                    : null;
            case "LESSTHANOREQUAL" -> value != null ? lte(resolveFieldName(field), value) : null;
            case "LESSTHAN" -> value != null ? lt(resolveFieldName(field), value) : null;
            case "GREATERTHANOREQUAL" -> value != null ? gte(resolveFieldName(field), value) : null;
            case "GREATERTHAN" -> value != null ? gt(resolveFieldName(field), value) : null;
            case "ISNULL" -> eq(resolveFieldName(field), null);
            case "ISNOTNULL" -> exists(resolveFieldName(field));
            case "ISEMPTY" -> eq(resolveFieldName(field), "");
            case "ISNOTEMPTY" -> not(eq(resolveFieldName(field), ""));
            case "CONTAINS" -> value != null ? contains(resolveFieldName(field), value) : null;
            case "NOTCONTAINS" -> value != null ? notContains(resolveFieldName(field), value) : null;
            default -> throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        };
    }

    protected Bson contains(String field, Object value) {
        if (value instanceof Collection<?> collection) {
            return in(field, collection);
        }
        return in(field, value);
    }

    protected Bson notContains(String field, Object value) {
        if (value instanceof Collection<?> collection) {
            return nin(field, collection);
        }
        return nin(field, value);
    }

    protected String resolveFieldName(String field) {
        return "id".equals(field) ? "_id" : field;
    }


    private Class<?> resolveIdentifierType(Class<?> type) {
        Field field = identifierField(type);
        return field == null ? String.class : field.getType();
    }

    public Object convertIdentifier(String value, Class<?> targetType) {
        if (targetType == null || targetType == String.class || targetType.isAssignableFrom(String.class)) {
            return value;
        }
        if (targetType == ObjectId.class) {
            return new ObjectId(value);
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
        throw new CoredeuxValidationException("Unsupported identifier type: " + targetType.getName());
    }

    public Object invokeStringFactory(Class<?> targetType, String value) {
        return null;
    }

    protected Object generateIdentifier(Class<?> type, Field idField) {
        Class<?> targetType = idField.getType();
        if (targetType == String.class) {
            return new ObjectId().toHexString();
        }
        if (targetType == ObjectId.class) {
            return new ObjectId();
        }
        if (targetType == UUID.class) {
            return UUID.randomUUID();
        }
        if (targetType == Long.class || targetType == long.class) {
            return new ObjectId().getTimestamp() & 0x7fffffffL;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return (int) (new ObjectId().getTimestamp() & 0x7fffffffL);
        }
        if (targetType == Short.class || targetType == short.class) {
            return (short) (new ObjectId().getTimestamp() & 0x7fff);
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return (byte) (new ObjectId().getTimestamp() & 0x7f);
        }
        if (targetType == BigInteger.class) {
            return BigInteger.valueOf(new ObjectId().getTimestamp() & 0x7fffffffL);
        }
        if (targetType == BigDecimal.class) {
            return BigDecimal.valueOf(new ObjectId().getTimestamp() & 0x7fffffffL);
        }
        return new ObjectId().toHexString();
    }

    protected String resolveQueryTemplate(String query, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return query;
        }

        Matcher matcher = TEMPLATE_PATTERN.matcher(query);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!params.containsKey(key)) {
                throw new CoredeuxValidationException("Missing query parameter: " + key);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(toJsonLiteral(params.get(key))));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    protected String toJsonLiteral(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new CoredeuxValidationException("Unable to serialize query parameter value", exception);
        }
    }

    protected Document toDocument(Object entity) {
        return normalizeDocument(Document.parse(writeJson(entity)));
    }

    protected <T> T fromDocument(Document document, Class<T> type) {
        try {
            return objectMapper.readValue(document.toJson(), type);
        } catch (Exception exception) {
            throw new CoredeuxDataAccessException("Unable to deserialize Mongo document for type " + type.getName(),
                    exception);
        }
    }

    protected Document normalizeDocumentId(Document document, Class<?> type) {
        Document copy = new Document(document);
        if (copy.containsKey("_id")) {
            copy.put("id", copy.remove("_id"));
        }
        return copy;
    }

    protected Document normalizeDocument(Document document) {
        if (document.containsKey("id") && !document.containsKey("_id")) {
            document.put("_id", document.get("id"));
        }
        return document;
    }

    protected String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new CoredeuxDataAccessException("Unable to serialize Mongo entity", exception);
        }
    }

    protected MongoCollection<Document> collection(Class<?> type) {
        return database.getCollection(collectionName(type));
    }

    protected String collectionName(Class<?> type) {
        String annotationName = mongoDocumentName(type);
        return annotationName == null || annotationName.isBlank() ? type.getSimpleName() : annotationName;
    }

    protected String mongoDocumentName(Class<?> type) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends java.lang.annotation.Annotation> documentType =
                    (Class<? extends java.lang.annotation.Annotation>) Class
                            .forName("org.springframework.data.mongodb.core.mapping.Document");
            java.lang.annotation.Annotation annotation = type.getAnnotation(documentType);
            if (annotation == null) {
                return null;
            }
            Object value = annotationTypeValue(annotation);
            return value == null ? null : String.valueOf(value);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    protected Object annotationTypeValue(java.lang.annotation.Annotation annotation) {
        try {
            return annotation.annotationType().getMethod("value").invoke(annotation);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    protected Bson filterById(Class<?> type, Object identifier) {
        return eq("_id", identifier);
    }

    protected Object normalizeMongoId(Object identifier, Class<?> targetType) {
        if (targetType == ObjectId.class && identifier instanceof String stringIdentifier
                && ObjectId.isValid(stringIdentifier)) {
            return new ObjectId(stringIdentifier);
        }
        return identifier;
    }

    protected boolean fieldHasAnnotation(Field field, String annotationClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends java.lang.annotation.Annotation> annotationType =
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName(annotationClassName);
            return field.isAnnotationPresent(annotationType);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    protected void copyProperties(Object source, Object target) {
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
