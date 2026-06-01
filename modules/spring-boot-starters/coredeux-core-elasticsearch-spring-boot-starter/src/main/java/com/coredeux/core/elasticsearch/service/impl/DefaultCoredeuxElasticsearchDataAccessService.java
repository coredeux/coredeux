package com.coredeux.core.elasticsearch.service.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
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
 * Elasticsearch-backed implementation of {@link CoredeuxDataAccessService}.
 */
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ElasticsearchOperations elasticsearchOperations;
    private final ConcurrentMap<Class<?>, Field> identifierFieldCache = new ConcurrentHashMap<>();
    private final String defaultIndexPrefix;

    public DefaultCoredeuxElasticsearchDataAccessService(ElasticsearchOperations elasticsearchOperations,
            String defaultIndexPrefix) {
        this.elasticsearchOperations = Objects.requireNonNull(elasticsearchOperations, "elasticsearchOperations");
        this.defaultIndexPrefix = defaultIndexPrefix == null ? "" : defaultIndexPrefix.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public <T> T load(String id, Class<T> type) {
        validateLoadInput(id, type);
        try {
            IndexCoordinates coordinates = indexCoordinates(type);
            T entity = elasticsearchOperations.get(id, type, coordinates);
            return entity;
        } catch (RuntimeException exception) {
            throw wrap("Unable to load entity of type " + typeName(type) + " for identifier '" + id + "'", exception);
        }
    }

    @Override
    @Transactional
    public <T> String save(T entity) {
        validateEntity(entity, "save");
        try {
            IndexCoordinates coordinates = indexCoordinates(entity.getClass());
            T saved = Objects.requireNonNull(elasticsearchOperations.save(entity, coordinates),
                    "Elasticsearch save response must not be null");
            Object identifier = extractIdentifier(saved);
            setIdentifier(entity, identifier);
            return identifier == null ? null : String.valueOf(identifier);
        } catch (RuntimeException exception) {
            throw wrap("Unable to save entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    @Transactional
    public <T> void update(T entity) {
        validateEntity(entity, "update");
        try {
            IndexCoordinates coordinates = indexCoordinates(entity.getClass());
            T saved = Objects.requireNonNull(elasticsearchOperations.save(entity, coordinates),
                    "Elasticsearch save response must not be null");
            Object identifier = extractIdentifier(saved);
            if (identifier != null) {
                setIdentifier(entity, identifier);
            }
        } catch (RuntimeException exception) {
            throw wrap("Unable to update entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    @Transactional
    public <T> void remove(T entity) {
        validateEntity(entity, "remove");
        try {
            IndexCoordinates coordinates = indexCoordinates(entity.getClass());
            elasticsearchOperations.delete(entity, coordinates);
        } catch (RuntimeException exception) {
            throw wrap("Unable to remove entity of type " + entity.getClass().getName(), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        validateSearchType(type);
        try {
            IndexCoordinates coordinates = indexCoordinates(type);
            Query countQuery = buildStructuredQuery(params);
            Query dataQuery = buildStructuredQuery(params);
            applyPaging(dataQuery, pageSize, currentPage);

            long totalResults = elasticsearchOperations.count(Query.findAll(), type, coordinates);
            long filteredResults = elasticsearchOperations.count(countQuery, type, coordinates);
            SearchHits<T> hits = elasticsearchOperations.search(dataQuery, type, coordinates);

            List<T> results = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

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
            String resolvedQuery = resolveQueryTemplate(query, params);
            IndexCoordinates coordinates = indexCoordinates(type);
            StringQuery countQuery = new StringQuery(resolvedQuery);
            StringQuery dataQuery = new StringQuery(resolvedQuery);
            applyPaging(dataQuery, pageSize, currentPage);

            long totalResults = elasticsearchOperations.count(countQuery, type, coordinates);
            SearchHits<T> hits = elasticsearchOperations.search(dataQuery, type, coordinates);
            List<T> results = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

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

    protected IndexCoordinates indexCoordinates(Class<?> type) {
        IndexCoordinates coordinates = elasticsearchOperations.getIndexCoordinatesFor(type);
        if (coordinates != null) {
            return coordinates;
        }
        String indexName = type.getSimpleName();
        if (!defaultIndexPrefix.isBlank()) {
            indexName = defaultIndexPrefix + "-" + indexName;
        }
        return IndexCoordinates.of(indexName);
    }

    protected Query buildStructuredQuery(List<SearchParams> params) {
        if (CollectionUtils.isEmpty(params)) {
            return new StringQuery("{\"match_all\":{}}");
        }

        Criteria criteria = null;
        for (SearchParams searchParams : params) {
            if (searchParams == null) {
                continue;
            }
            Criteria current = buildCriteria(searchParams);
            if (current == null) {
                continue;
            }
            criteria = criteria == null ? current : criteria.and(current);
        }

        if (criteria == null) {
            return new StringQuery("{\"match_all\":{}}");
        }
        return new CriteriaQuery(criteria);
    }

    protected Criteria buildCriteria(SearchParams searchParams) {
        String field = normalizeRequired(searchParams.getField(), "Search field must not be blank");
        String comparator = normalizeRequired(searchParams.getComparator(), "Search comparator must not be blank")
                .toUpperCase(Locale.ROOT);
        Object value = searchParams.getValue();

        if (!SUPPORTED_COMPARATORS.contains(comparator)) {
            throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        }

        Criteria criteria = Criteria.where(field);
        return switch (comparator) {
            case "EQUALS" -> value != null ? criteria.is(value) : null;
            case "NOTEQUALS" -> value != null ? criteria.not().is(value) : null;
            case "STARTSWITH" -> value != null ? criteria.startsWith(String.valueOf(value)) : null;
            case "ANYWHERECS" -> value != null ? criteria.contains(String.valueOf(value)) : null;
            case "ANYWHERE" -> value != null ? criteria.contains(String.valueOf(value)) : null;
            case "LESSTHANOREQUAL" -> value != null ? criteria.lessThanEqual(value) : null;
            case "LESSTHAN" -> value != null ? criteria.lessThan(value) : null;
            case "GREATERTHANOREQUAL" -> value != null ? criteria.greaterThanEqual(value) : null;
            case "GREATERTHAN" -> value != null ? criteria.greaterThan(value) : null;
            case "ISNULL" -> criteria.not().exists();
            case "ISNOTNULL" -> criteria.exists();
            case "ISEMPTY" -> criteria.empty();
            case "ISNOTEMPTY" -> criteria.notEmpty();
            case "CONTAINS" -> value != null ? contains(criteria, value) : null;
            case "NOTCONTAINS" -> value != null ? notContains(criteria, value) : null;
            default -> throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        };
    }

    protected Criteria contains(Criteria criteria, Object value) {
        if (value instanceof Iterable<?> iterable) {
            return criteria.in(iterable);
        }
        return criteria.contains(String.valueOf(value));
    }

    protected Criteria notContains(Criteria criteria, Object value) {
        if (value instanceof Iterable<?> iterable) {
            return criteria.notIn(iterable);
        }
        return criteria.not().contains(String.valueOf(value));
    }

    protected void applyPaging(Query query, int pageSize, int currentPage) {
        if (!isPagingEnabled(pageSize, currentPage)) {
            return;
        }
        query.setPageable(PageRequest.of(Math.max(currentPage - 1, 0), pageSize));
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
        Field annotated = findAnnotatedField(type, Id.class);
        if (annotated != null) {
            return annotated;
        }
        Field fallback = ReflectionUtils.findField(type, "id");
        if (fallback == null) {
            throw new CoredeuxValidationException("Unable to resolve identifier field for class: " + type.getName());
        }
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
}
