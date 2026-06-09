package com.coredeux.drl.service;

import com.coredeux.drl.model.RuleContext;

/**
 * Runtime service for executing compiled or ad hoc DRL rules.
 *
 * <p>The service resolves rules by identifier, compiles caller-provided DRL
 * when requested, and updates the supplied {@link RuleContext} with execution
 * results.
 */
public interface DRLService {

    /**
     * Resolves, compiles, caches, and executes the rule identified by the given
     * rule id.
     *
     * @param ruleId the rule identifier used by the configured source resolver
     * @param context the execution context and fact carrier
     */
    <T> void execute(String ruleId, RuleContext<T> context);

    /**
     * Compiles the supplied DRL source, stores the compiled rule base under the
     * given rule id, and executes it immediately.
     *
     * @param ruleId the cache key to use for the compiled rule base
     * @param source the DRL source text to compile
     * @param context the execution context and fact carrier
     */
    <T> void execute(String ruleId, String source, RuleContext<T> context);

    /**
     * Compiles and executes the supplied DRL source without consulting a source
     * resolver or storing the compiled result in the cache.
     *
     * @param source the DRL source text to compile
     * @param context the execution context and fact carrier
     */
    <T> void executeSource(String source, RuleContext<T> context);

    /**
     * Clears all cached compiled rule bases.
     */
    void purgeCache();

    /**
     * Removes a single cached compiled rule base.
     *
     * @param ruleId the cache key to remove
     */
    void purgeCache(String ruleId);

    /**
     * Checks whether a compiled rule base is currently cached for the rule id.
     *
     * @param ruleId the cache key to check
     * @return {@code true} if a compiled rule base is present in the cache
     */
    boolean isCached(String ruleId);
}
