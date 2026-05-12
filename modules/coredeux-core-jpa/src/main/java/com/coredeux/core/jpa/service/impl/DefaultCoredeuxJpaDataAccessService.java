package com.coredeux.core.jpa.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.jpa.support.JpaIdentifierConverter;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;

/**
 * Default JPA-backed implementation of {@link CoredeuxDataAccessService}.
 */
public class DefaultCoredeuxJpaDataAccessService implements CoredeuxDataAccessService, AutoCloseable {

    protected static final String EQUALS = "EQUALS";
    protected static final String NOTEQUALS = "NOTEQUALS";
    protected static final String STARTSWITH = "STARTSWITH";
    protected static final String ANYWHERECS = "ANYWHERECS";
    protected static final String ANYWHERE = "ANYWHERE";
    protected static final String LESSTHANOREQUAL = "LESSTHANOREQUAL";
    protected static final String LESSTHAN = "LESSTHAN";
    protected static final String GREATERTHANOREQUAL = "GREATERTHANOREQUAL";
    protected static final String GREATERTHAN = "GREATERTHAN";
    protected static final String ISNULL = "ISNULL";
    protected static final String ISNOTNULL = "ISNOTNULL";
    protected static final String ISEMPTY = "ISEMPTY";
    protected static final String ISNOTEMPTY = "ISNOTEMPTY";
    protected static final String CONTAINS = "CONTAINS";
    protected static final String NOTCONTAINS = "NOTCONTAINS";

    private static final Set<String> SUPPORTED_COMPARATORS = Set.of(EQUALS, NOTEQUALS, STARTSWITH, ANYWHERECS,
            ANYWHERE, LESSTHANOREQUAL, LESSTHAN, GREATERTHANOREQUAL, GREATERTHAN, ISNULL, ISNOTNULL, ISEMPTY,
            ISNOTEMPTY, CONTAINS, NOTCONTAINS);

    private EntityManager entityManager;
    private EntityManagerFactory entityManagerFactory;
    private final ThreadLocal<EntityManager> activeEntityManager = new ThreadLocal<>();

    public DefaultCoredeuxJpaDataAccessService() {
    }

    public DefaultCoredeuxJpaDataAccessService(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
    }

    public DefaultCoredeuxJpaDataAccessService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "entityManagerFactory must not be null");
    }

    @Override
    public <T> T load(String id, Class<T> type) {
        validateLoadInput(id, type);
        return read(entityManager -> {
            Object identifier = JpaIdentifierConverter.convert(id, resolveIdentifierType(type));
            T entity = entityManager.find(type, identifier);
            if (entity != null) {
                entityManager.detach(entity);
            }
            return entity;
        }, "Unable to load entity of type " + type.getName() + " for identifier '" + id + "'");
    }

    @Override
    public <T> String save(T entity) {
        validateEntity(entity, "save");
        return write(entityManager -> {
            entityManager.persist(entity);
            entityManager.flush();
            Object identifier = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
            return identifier != null ? String.valueOf(identifier) : null;
        }, "Unable to save entity of type " + entity.getClass().getName());
    }

    @Override
    public <T> void update(T entity) {
        validateEntity(entity, "update");
        write(entityManager -> {
            entityManager.merge(entity);
            entityManager.flush();
            return null;
        }, "Unable to update entity of type " + entity.getClass().getName());
    }

    @Override
    public <T> void remove(T entity) {
        validateEntity(entity, "remove");
        write(entityManager -> {
            T managedEntity = entityManager.contains(entity) ? entity : entityManager.merge(entity);
            entityManager.remove(managedEntity);
            entityManager.flush();
            return null;
        }, "Unable to remove entity of type " + entity.getClass().getName());
    }

    @Override
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        validateSearchType(type);
        return read(entityManager -> {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> dataQuery = criteriaBuilder.createQuery(type);
            Root<T> root = dataQuery.from(type);
            List<Predicate> predicates = buildPredicates(params, criteriaBuilder, root);
            dataQuery.select(root);
            if (!predicates.isEmpty()) {
                dataQuery.where(predicates.toArray(Predicate[]::new));
            }

            TypedQuery<T> typedQuery = entityManager.createQuery(dataQuery);
            long totalResults = countAll(type);
            long filteredResults = countFiltered(type, params);
            applyPaging(typedQuery, pageSize, currentPage);
            List<T> results = typedQuery.getResultList();
            detachResults(results);
            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, filteredResults, pageSize, currentPage))
                    .build();
        }, "Unable to load entities for type " + type.getName());
    }

    @Override
    public Set<String> supportedComparators(Class<?> type) {
        return SUPPORTED_COMPARATORS;
    }

    @Override
    public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage) {
        validateQueryInput(query, type);
        return read(entityManager -> {
            TypedQuery<T> countQuery = entityManager.createQuery(query, type);
            bindParameters(countQuery, params);
            long totalResults = countQuery.getResultList().size();

            TypedQuery<T> dataQuery = entityManager.createQuery(query, type);
            bindParameters(dataQuery, params);
            applyPaging(dataQuery, pageSize, currentPage);
            List<T> results = dataQuery.getResultList();
            detachResults(results);

            return SearchResult.<T>builder()
                    .results(defaultResults(results))
                    .pagination(buildPagination(totalResults, totalResults, pageSize, currentPage))
                    .build();
        }, "Unable to execute query for type " + type.getName());
    }

    @Override
    public <T> void refresh(T entity) {
        validateEntity(entity, "refresh");
        write(entityManager -> {
            T managedEntity = entityManager.contains(entity) ? entity : entityManager.merge(entity);
            entityManager.refresh(managedEntity);
            return null;
        }, "Unable to refresh entity of type " + entity.getClass().getName());
    }

    @Override
    public void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }

    void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected EntityManager getEntityManager() {
        if (entityManager != null) {
            return entityManager;
        }
        EntityManager currentEntityManager = activeEntityManager.get();
        if (currentEntityManager == null) {
            throw new CoredeuxDataAccessException("EntityManager is not configured");
        }
        return currentEntityManager;
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

    protected <T> Class<?> resolveIdentifierType(Class<T> type) {
        EntityType<T> entityType = getEntityManager().getMetamodel().entity(type);
        if (!entityType.hasSingleIdAttribute()) {
            throw new CoredeuxValidationException(
                    "Composite identifiers are not supported by DefaultCoredeuxJpaDataAccessService for class: "
                            + type.getName());
        }
        return entityType.getIdType().getJavaType();
    }

    protected void bindParameters(TypedQuery<?> query, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        params.forEach(query::setParameter);
    }

    protected void applyPaging(TypedQuery<?> query, int pageSize, int currentPage) {
        if (!isPagingEnabled(pageSize, currentPage)) {
            return;
        }
        query.setFirstResult(Math.max(currentPage - 1, 0) * pageSize);
        query.setMaxResults(pageSize);
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
        return results == null || results.isEmpty() ? List.of() : results;
    }

    protected <T> void detachResults(List<T> results) {
        if (results == null) {
            return;
        }
        EntityManager currentEntityManager = getEntityManager();
        for (T result : results) {
            if (result != null && currentEntityManager.contains(result)) {
                currentEntityManager.detach(result);
            }
        }
    }

    protected <T> long countAll(Class<T> type) {
        EntityManager currentEntityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = currentEntityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<T> root = query.from(type);
        query.select(criteriaBuilder.count(root));
        return currentEntityManager.createQuery(query).getSingleResult();
    }

    protected <T> long countFiltered(Class<T> type, List<SearchParams> params) {
        EntityManager currentEntityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = currentEntityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<T> root = query.from(type);
        List<Predicate> predicates = buildPredicates(params, criteriaBuilder, root);
        query.select(criteriaBuilder.count(root));
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }
        return currentEntityManager.createQuery(query).getSingleResult();
    }

    protected <T> List<Predicate> buildPredicates(List<SearchParams> params, CriteriaBuilder criteriaBuilder,
            Root<T> root) {
        if (params == null || params.isEmpty()) {
            return List.of();
        }
        List<Predicate> predicates = new ArrayList<>();
        for (SearchParams searchParam : params) {
            if (searchParam == null) {
                continue;
            }
            Predicate predicate = buildPredicate(searchParam, criteriaBuilder, root);
            if (predicate != null) {
                predicates.add(predicate);
            }
        }
        return predicates;
    }

    protected Predicate buildPredicate(SearchParams searchParam, CriteriaBuilder criteriaBuilder, Root<?> root) {
        String field = normalizeRequired(searchParam.getField(), "Search field must not be blank");
        String comparator = normalizeRequired(searchParam.getComparator(),
                "Search comparator must not be blank").toUpperCase(Locale.ROOT);
        Object value = searchParam.getValue();
        Path<?> path = resolvePath(root, field);

        return switch (comparator) {
            case EQUALS -> value != null ? criteriaBuilder.equal(path, value) : null;
            case NOTEQUALS -> value != null ? criteriaBuilder.notEqual(path, value) : null;
            case STARTSWITH -> like(criteriaBuilder, path, value, false, true);
            case ANYWHERECS -> like(criteriaBuilder, path, value, false, false);
            case ANYWHERE -> like(criteriaBuilder, path, value, true, false);
            case LESSTHANOREQUAL -> compare(criteriaBuilder, path, value, ComparisonType.LESS_THAN_OR_EQUAL);
            case LESSTHAN -> compare(criteriaBuilder, path, value, ComparisonType.LESS_THAN);
            case GREATERTHANOREQUAL -> compare(criteriaBuilder, path, value, ComparisonType.GREATER_THAN_OR_EQUAL);
            case GREATERTHAN -> compare(criteriaBuilder, path, value, ComparisonType.GREATER_THAN);
            case ISNULL -> criteriaBuilder.isNull(path);
            case ISNOTNULL -> criteriaBuilder.isNotNull(path);
            case ISEMPTY -> criteriaBuilder.isEmpty(asCollectionExpression(path, comparator));
            case ISNOTEMPTY -> criteriaBuilder.isNotEmpty(asCollectionExpression(path, comparator));
            case CONTAINS -> value != null ? criteriaBuilder.isMember(value, asCollectionExpression(path, comparator)) : null;
            case NOTCONTAINS -> value != null ? criteriaBuilder.isNotMember(value, asCollectionExpression(path, comparator)) : null;
            default -> throw new CoredeuxValidationException("Unsupported search comparator: " + comparator);
        };
    }

    protected String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new CoredeuxValidationException(message);
        }
        return value.trim();
    }

    @SuppressWarnings("unchecked")
    protected Expression<Collection<Object>> asCollectionExpression(Path<?> path, String comparator) {
        if (!(path.getModel() instanceof PluralAttribute<?, ?, ?>) && !Collection.class.isAssignableFrom(path.getJavaType())) {
            throw new CoredeuxValidationException(
                    comparator + " comparator requires a collection-valued field but got: " + path.getJavaType().getName());
        }
        return (Expression<Collection<Object>>) path;
    }

    protected Predicate like(CriteriaBuilder criteriaBuilder, Path<?> path, Object value, boolean ignoreCase,
            boolean startsWith) {
        if (value == null) {
            return null;
        }
        String stringValue = String.valueOf(value);
        String pattern = startsWith ? stringValue + "%" : "%" + stringValue + "%";
        Expression<String> expression = path.as(String.class);
        if (ignoreCase) {
            return criteriaBuilder.like(criteriaBuilder.lower(expression), pattern.toLowerCase(Locale.ROOT));
        }
        return criteriaBuilder.like(expression, pattern);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Predicate compare(CriteriaBuilder criteriaBuilder, Path<?> path, Object value, ComparisonType comparisonType) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Comparable comparable)) {
            throw new CoredeuxValidationException("Comparator value must implement Comparable: " + value.getClass().getName());
        }
        Expression<? extends Comparable> expression = (Expression<? extends Comparable>) path;
        return switch (comparisonType) {
            case LESS_THAN -> criteriaBuilder.lessThan(expression, comparable);
            case LESS_THAN_OR_EQUAL -> criteriaBuilder.lessThanOrEqualTo(expression, comparable);
            case GREATER_THAN -> criteriaBuilder.greaterThan(expression, comparable);
            case GREATER_THAN_OR_EQUAL -> criteriaBuilder.greaterThanOrEqualTo(expression, comparable);
        };
    }

    protected Path<?> resolvePath(Path<?> path, String field) {
        Path<?> current = path;
        for (String part : field.split("\\.")) {
            current = current.get(part.trim());
        }
        return current;
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

    protected <T> T read(Function<EntityManager, T> callback, String errorMessage) {
        if (entityManagerFactory == null) {
            try {
                return callback.apply(getEntityManager());
            } catch (RuntimeException exception) {
                throw wrap(errorMessage, exception);
            }
        }

        EntityManager currentEntityManager = entityManagerFactory.createEntityManager();
        activeEntityManager.set(currentEntityManager);
        try {
            return callback.apply(currentEntityManager);
        } catch (RuntimeException exception) {
            throw wrap(errorMessage, exception);
        } finally {
            activeEntityManager.remove();
            currentEntityManager.close();
        }
    }

    protected <T> T write(Function<EntityManager, T> callback, String errorMessage) {
        if (entityManagerFactory == null) {
            try {
                return callback.apply(getEntityManager());
            } catch (RuntimeException exception) {
                throw wrap(errorMessage, exception);
            }
        }

        EntityManager currentEntityManager = entityManagerFactory.createEntityManager();
        activeEntityManager.set(currentEntityManager);
        EntityTransaction transaction = currentEntityManager.getTransaction();
        try {
            transaction.begin();
            T result = callback.apply(currentEntityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw wrap(errorMessage, exception);
        } finally {
            activeEntityManager.remove();
            currentEntityManager.close();
        }
    }

    protected enum ComparisonType {
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL
    }
}
