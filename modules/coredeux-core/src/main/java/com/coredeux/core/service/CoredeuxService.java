package com.coredeux.core.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

/**
 * Public Coredeux service contract.
 * Implementations coordinate validation, authorization, strategy selection,
 * audit concerns, and persistence access while exposing a stable API to callers.
 */
public interface CoredeuxService {

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
     * Executes a query.
     *
     * @param query the query expression to execute
     * @param params the named query parameters
     * @param type the entity type
     * @param pageSize the number of results per page
     * @param currentPage the requested page number
     * @param <T> the entity type
     * @return the paged query result
     */
    <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize, int currentPage);

    /**
     * Loads entities using structured filters.
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
     * Returns the structured-search comparators supported by the data access
     * service selected for the given entity type.
     *
     * @param type the entity type
     * @return supported comparator names
     */
    default Set<String> supportedComparators(Class<?> type) {
        return Set.of("EQUALS");
    }

    /**
     * Persists an entity.
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
     * Removes an entity by identifier.
     *
     * @param id the unique identifier of the entity
     * @param type the entity type
     * @param <T> the entity type
     */
    <T> void remove(String id, Class<T> type);

    /**
     * Removes an existing entity.
     *
     * @param entity the entity to remove
     * @param <T> the entity type
     */
    <T> void remove(T entity);

    /**
     * Refreshes the given entity.
     *
     * @param entity the entity to refresh
     * @param <T> the entity type
     */
    <T> void refresh(T entity);
}
