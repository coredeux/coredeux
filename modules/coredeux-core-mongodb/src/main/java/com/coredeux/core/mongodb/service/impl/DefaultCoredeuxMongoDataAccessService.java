package com.coredeux.core.mongodb.service.impl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.PageRequest;
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
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MongoDB-backed implementation of {@link CoredeuxDataAccessService}.
 */
@Service("defaultCoredeuxMongoDataAccessService")
public class DefaultCoredeuxMongoDataAccessService implements CoredeuxDataAccessService {

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

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MongoTemplate mongoTemplate;

    public DefaultCoredeuxMongoDataAccessService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate");
    }

    @Override
    @Transactional(readOnly = true)
    public <T> T load(String id, Class<T> type) {
        validateLoadInput(id, type);
        try {
            Object convertedId = convertIdentifier(id, resolveIdentifierType(type));
            T entity = mongoTemplate.findById(convertedId, type);
            return entity;
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entity of type " + type.getName() + " for identifier '" + id + "'", exception);
        }
    }

    @Override
    @Transactional
    public <T> String save(T entity) {
        validateEntity(entity, "save");
        try {
            T saved = mongoTemplate.save(entity);
            Object identifier = extractIdentifier(saved);
            return identifier == null ? null : String.valueOf(identifier);
        } catch (RuntimeException exception) {
            throw wrap("Unable to save entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    @Transactional
    public <T> void update(T entity) {
        validateEntity(entity, "update");
        requireIdentifier(entity, "update");
        try {
            mongoTemplate.save(entity);
        } catch (RuntimeException exception) {
            throw wrap("Unable to update entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    @Transactional
    public <T> void remove(T entity) {
        validateEntity(entity, "remove");
        requireIdentifier(entity, "remove");
        try {
            mongoTemplate.remove(entity);
        } catch (RuntimeException exception) {
            throw wrap("Unable to remove entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        validateSearchType(type);
        try {
            Query countQuery = buildSearchQuery(params);
            long totalResults = mongoTemplate.count(new Query(), type);
            long filteredResults = mongoTemplate.count(countQuery, type);

            Query dataQuery = buildSearchQuery(params);
            applyPaging(dataQuery, pageSize, currentPage);
            List<T> results = mongoTemplate.find(dataQuery, type);

            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, filteredResults, pageSize, currentPage))
                    .build();
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entities for type " + type.getName(), exception);
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
            String resolvedQuery = resolveQueryTemplate(query, params);
            Query countQuery = new BasicQuery(resolvedQuery);
            long totalResults = mongoTemplate.count(countQuery, type);

            Query dataQuery = new BasicQuery(resolvedQuery);
            applyPaging(dataQuery, pageSize, currentPage);
            List<T> results = mongoTemplate.find(dataQuery, type);

            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, totalResults, pageSize, currentPage))
                    .build();
        } catch (RuntimeException exception) {
            throw wrap("Unable to execute query for type " + type.getName(), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public <T> void refresh(T entity) {
        validateEntity(entity, "refresh");
        Object identifier = requireIdentifier(entity, "refresh");
        try {
            @SuppressWarnings("unchecked")
            T refreshed = (T) mongoTemplate.findById(identifier, entity.getClass());
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

    protected Query buildSearchQuery(List<SearchParams> params) {
        if (CollectionUtils.isEmpty(params)) {
            return new Query();
        }

        List<Criteria> criteria = new ArrayList<>();
        for (SearchParams searchParams : params) {
            if (searchParams == null) {
                continue;
            }
            Criteria criterion = buildCriteria(searchParams);
            if (criterion != null) {
                criteria.add(criterion);
            }
        }

        if (criteria.isEmpty()) {
            return new Query();
        }
        Query query = new Query();
        if (criteria.size() == 1) {
            query.addCriteria(criteria.get(0));
            return query;
        }
        query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        return query;
    }

    protected Criteria buildCriteria(SearchParams searchParams) {
        String field = normalizeRequired(searchParams.getField(), "Search field must not be blank");
        String comparator = normalizeRequired(searchParams.getComparator(), "Search comparator must not be blank")
                .toUpperCase(Locale.ROOT);
        Object value = searchParams.getValue();

        if (!SUPPORTED_COMPARATORS.contains(comparator)) {
            throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        }

        return switch (comparator) {
            case "EQUALS" -> value != null ? Criteria.where(field).is(value) : null;
            case "NOTEQUALS" -> value != null ? Criteria.where(field).ne(value) : null;
            case "STARTSWITH" -> value != null ? Criteria.where(field).regex("^" + Pattern.quote(String.valueOf(value))) : null;
            case "ANYWHERECS" -> value != null
                    ? Criteria.where(field).regex(".*" + Pattern.quote(String.valueOf(value)) + ".*")
                    : null;
            case "ANYWHERE" -> value != null
                    ? Criteria.where(field).regex(Pattern.compile(".*" + Pattern.quote(String.valueOf(value)) + ".*",
                            Pattern.CASE_INSENSITIVE))
                    : null;
            case "LESSTHANOREQUAL" -> value != null ? compare(field, value, ComparisonType.LESS_THAN_OR_EQUAL) : null;
            case "LESSTHAN" -> value != null ? compare(field, value, ComparisonType.LESS_THAN) : null;
            case "GREATERTHANOREQUAL" -> value != null ? compare(field, value, ComparisonType.GREATER_THAN_OR_EQUAL) : null;
            case "GREATERTHAN" -> value != null ? compare(field, value, ComparisonType.GREATER_THAN) : null;
            case "ISNULL" -> Criteria.where(field).is(null);
            case "ISNOTNULL" -> Criteria.where(field).ne(null).exists(true);
            case "ISEMPTY" -> Criteria.where(field).size(0);
            case "ISNOTEMPTY" -> Criteria.where(field).not().size(0);
            case "CONTAINS" -> value != null ? Criteria.where(field).in(value) : null;
            case "NOTCONTAINS" -> value != null ? Criteria.where(field).nin(value) : null;
            default -> throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        };
    }

    protected Criteria compare(String field, Object value, ComparisonType comparisonType) {
        if (!(value instanceof Comparable<?> comparable)) {
            throw new CoredeuxValidationException(
                    "Comparator value must implement Comparable: " + value.getClass().getName());
        }
        return switch (comparisonType) {
            case LESS_THAN -> Criteria.where(field).lt(comparable);
            case LESS_THAN_OR_EQUAL -> Criteria.where(field).lte(comparable);
            case GREATER_THAN -> Criteria.where(field).gt(comparable);
            case GREATER_THAN_OR_EQUAL -> Criteria.where(field).gte(comparable);
        };
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

    protected void applyPaging(Query query, int pageSize, int currentPage) {
        if (!isPagingEnabled(pageSize, currentPage)) {
            return;
        }
        query.with(PageRequest.of(Math.max(currentPage - 1, 0), pageSize));
    }

    protected boolean isPagingEnabled(int pageSize, int currentPage) {
        return pageSize > 0 && currentPage > 0;
    }

    protected Object requireIdentifier(Object entity, String action) {
        Object identifier = extractIdentifier(entity);
        if (identifier == null || (identifier instanceof String stringIdentifier && stringIdentifier.isBlank())) {
            throw new CoredeuxValidationException("Entity identifier must not be blank for " + action);
        }
        return identifier;
    }

    protected Object extractIdentifier(Object entity) {
        Field field = findIdentifierField(entity.getClass());
        if (field == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, entity);
    }

    protected Class<?> resolveIdentifierType(Class<?> type) {
        Field field = findIdentifierField(type);
        if (field != null) {
            return field.getType();
        }
        return String.class;
    }

    protected Field findIdentifierField(Class<?> type) {
        Field annotatedField = findAnnotatedField(type, Id.class);
        if (annotatedField != null) {
            return annotatedField;
        }
        Field fallback = ReflectionUtils.findField(type, "id");
        return fallback;
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
                // keep searching
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
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            throw new CoredeuxValidationException("Unable to serialize query parameter value", exception);
        }
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

    protected enum ComparisonType {
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL
    }
}
