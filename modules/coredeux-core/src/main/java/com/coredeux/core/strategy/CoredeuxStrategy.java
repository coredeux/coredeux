package com.coredeux.core.strategy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

/**
 * Strategy contract for all core entity operations exposed by the public
 * service layer.
 */
public interface CoredeuxStrategy {

    /**
     * Loads a single entity using the configured data access service resolved by
     * the strategy.
     *
     * @param id the unique identifier of the entity
     * @param type the entity type
     * @param <T> the entity type
     * @return the loaded entity
     */
    <T> T load(String id, Class<T> type);

    /**
     * Executes a query string with named parameters.
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
     * Loads entities using structured search parameters.
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
     * service resolved for the given entity type.
     *
     * @param type the entity type
     * @return supported comparator names
     */
    default Set<String> supportedComparators(Class<?> type) {
        return Set.of("EQUALS");
    }

    /**
     * Persists a new entity using the configured data access service resolved by
     * the strategy.
     *
     * @param entity the entity to persist
     * @param <T> the entity type
     * @return the generated or resolved identifier of the saved entity
     */
    <T> String save(T entity);

    /**
     * Updates an entity using the configured data access service resolved by the
     * strategy.
     *
     * @param entity the entity to update
     * @param <T> the entity type
     */
    <T> void update(T entity);

    /**
     * Removes an entity by identifier using the configured data access service
     * resolved by the strategy.
     *
     * @param id the unique identifier of the entity
     * @param type the entity type
     * @param <T> the entity type
     */
    <T> void remove(String id, Class<T> type);

    /**
     * Removes an entity instance using the configured data access service resolved
     * by the strategy.
     *
     * @param entity the entity to remove
     * @param <T> the entity type
     */
    <T> void remove(T entity);

    /**
     * Refreshes an entity using the configured data access service resolved by the
     * strategy.
     *
     * @param entity the entity to refresh
     * @param <T> the entity type
     */
    <T> void refresh(T entity);
}
