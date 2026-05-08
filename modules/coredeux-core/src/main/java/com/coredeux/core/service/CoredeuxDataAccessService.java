package com.coredeux.core.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coredeux.core.exceptions.CoredeuxCoreException;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

/**
 * Data access contract for Coredeux.
 * Implementations should throw {@link CoredeuxCoreException} for framework-level
 * data access failures.
 */
public interface CoredeuxDataAccessService {

    /**
     * Loads a single entity by its identifier.
     *
     * @param id the unique identifier of the entity
     * @param type the entity type
     * @param <T> the entity type
     * @return the loaded entity
     */
    <T> T load(String id, Class<T> type);

    /**
     * Persists a new entity.
     *
     * @param entity the entity to persist
     * @param <T> the entity type
     * @return the generated or resolved identifier of the saved entity
     */
    <T> String save(T entity);

    /**
     * Updates an existing entity.
     *
     * @param entity the entity to update
     * @param <T> the entity type
     */
    <T> void update(T entity);

    /**
     * Removes an existing entity.
     *
     * @param entity the entity to remove
     * @param <T> the entity type
     */
    <T> void remove(T entity);

    /**
     * Loads entities using structured search parameters and pagination.
     *
     * @param params the search filters to apply
     * @param type the entity type
     * @param pageSize the number of results per page
     * @param currentPage the requested page number
     * @param <T> the entity type
     * @return the paged search result
     */
    <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage);

    /**
     * Returns the structured-search comparators supported by this data access
     * implementation for the given entity type.
     * <p>
     * Implementations may return a type-specific set when comparator support
     * depends on the target store, field model, or entity mapping. The default
     * exposes the framework's minimum equality comparator so existing
     * implementations remain source-compatible.
     *
     * @param type the entity type, or null when callers need implementation-wide
     *             defaults
     * @return supported comparator names
     */
    default Set<String> supportedComparators(Class<?> type) {
        return Set.of("EQUALS");
    }

    /**
     * Loads entities using a query string, named parameters, and pagination.
     *
     * @param query the query expression to execute
     * @param params the named query parameters
     * @param type the entity type
     * @param pageSize the number of results per page
     * @param currentPage the requested page number
     * @param <T> the entity type
     * @return the paged search result
     */
    <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage);

    /**
     * Refreshes the given entity from the underlying data source when supported.
     *
     * @param entity the entity to refresh
     * @param <T> the entity type
     */
    default <T> void refresh(T entity) {
    }
}
