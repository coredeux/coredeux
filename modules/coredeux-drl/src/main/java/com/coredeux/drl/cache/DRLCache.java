package com.coredeux.drl.cache;

import java.util.Optional;
import java.util.function.Supplier;

import org.kie.api.KieBase;

/**
 * Cache contract for compiled DRL bases.
 *
 * <p>Implementations store compiled {@link KieBase} instances by rule identifier
 * so callers can reuse compiled rules without rebuilding them on every
 * execution.
 */
public interface DRLCache {

    /**
     * Returns the cached compiled rule base for the given rule identifier.
     *
     * @param ruleId the rule identifier to look up
     * @return the cached {@link KieBase}, if present
     */
    Optional<KieBase> get(String ruleId);

    /**
     * Stores a compiled rule base under the given rule identifier.
     *
     * @param ruleId the cache key
     * @param kieBase the compiled rule base to store
     */
    void put(String ruleId, KieBase kieBase);

    /**
     * Returns the cached rule base if present, otherwise uses the loader to
     * compile and store one atomically.
     *
     * @param ruleId the cache key
     * @param loader supplier used to create the rule base when the cache is empty
     * @return the cached or newly loaded {@link KieBase}
     */
    KieBase computeIfAbsent(String ruleId, Supplier<KieBase> loader);

    /**
     * Indicates whether the cache currently contains the given rule identifier.
     *
     * @param ruleId the cache key to check
     * @return {@code true} when a rule base is cached for the key
     */
    boolean contains(String ruleId);

    /**
     * Removes a single cached rule base.
     *
     * @param ruleId the cache key to remove
     */
    void remove(String ruleId);

    /**
     * Clears the entire cache.
     */
    void clear();
}
