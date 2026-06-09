package com.coredeux.drl.core.service;

import java.util.Set;

import com.coredeux.core.search.SearchResult;
import com.coredeux.drl.model.RuleContext;

/**
 * DRL-backed data access contract used by pseudo Java rule sources.
 *
 * <p>Each method name is mapped from the data-access operation being executed
 * by the runtime, and implementations should populate the supplied rule
 * context output rather than returning a value directly.
 */
public interface DRLCoredeuxDataAccessService {

    /**
     * Loads a single entity by identifier.
     *
     * @param $context the rule execution context for the load operation
     */
	<T> void load(RuleContext<T> $context);

    /**
     * Persists the supplied entity and returns the persisted identifier through
     * the rule context output.
     *
     * @param $context the rule execution context for the save operation
     */
	void save(RuleContext<String> $context);

    /**
     * Updates the supplied entity.
     *
     * @param $context the rule execution context for the update operation
     */
	<T> void update(RuleContext<T> $context);

    /**
     * Removes the supplied entity.
     *
     * @param $context the rule execution context for the remove operation
     */
	<T> void remove(RuleContext<T> $context);

    /**
     * Loads a paginated result set.
     *
     * @param $context the rule execution context for the load-all operation
     */
	<T> void loadAll(RuleContext<SearchResult<T>> $context);

    /**
     * Produces the supported comparators for the target entity type.
     *
     * @param $context the rule execution context for the comparator lookup
     */
	void supportedComparators(RuleContext<Set<String>> $context);

    /**
     * Executes a search query and places the result into the rule context
     * output.
     *
     * @param $context the rule execution context for the query operation
     */
	<T> void query(RuleContext<SearchResult<T>> $context);

    /**
     * Refreshes the supplied entity.
     *
     * @param $context the rule execution context for the refresh operation
     */
	<T> void refresh(RuleContext<T> $context);
}
