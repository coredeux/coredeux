package com.coredeux.drl.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Thread-safe in-memory implementation of {@link DRLCache}.
 *
 * <p>This cache is suitable for native runtime usage and for test scenarios
 * where a simple concurrent map-backed cache is sufficient.
 */
public class InMemoryDRLCache implements DRLCache {

    private final Map<String, CompiledDRLRule> cache = new ConcurrentHashMap<>();

    /**
     * Retrieves the compiled rule base for the supplied rule identifier.
     *
     * @param ruleId the cache key to look up
     * @return the cached compiled rule, if present
     */
    @Override
    public Optional<CompiledDRLRule> get(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(ruleId));
    }

    /**
     * Stores the compiled rule base for the supplied rule identifier.
     *
     * @param ruleId the cache key to store
     * @param compiledRule the compiled rule bundle to cache
     */
    @Override
    public void put(String ruleId, CompiledDRLRule compiledRule) {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }
        if (compiledRule == null) {
            throw new IllegalArgumentException("compiledRule is required for ruleId: " + ruleId);
        }
        cache.put(ruleId, compiledRule);
    }

    /**
     * Loads and stores the compiled rule base only when no cache entry exists.
     *
     * @param ruleId the cache key to load
     * @param loader supplier used to build the compiled rule bundle on demand
     * @return the cached or newly loaded compiled rule bundle
     */
    @Override
    public CompiledDRLRule computeIfAbsent(String ruleId, Supplier<CompiledDRLRule> loader) {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }
        if (loader == null) {
            throw new IllegalArgumentException("loader is required for ruleId: " + ruleId);
        }
        return cache.computeIfAbsent(ruleId, key -> {
            CompiledDRLRule compiledRule = loader.get();
            if (compiledRule == null) {
                throw new IllegalStateException("Loader returned null compiled rule for ruleId: " + key);
            }
            return compiledRule;
        });
    }

    /**
     * Checks whether the given rule identifier is already cached.
     *
     * @param ruleId the cache key to test
     * @return {@code true} when a rule base is cached for the key
     */
    @Override
    public boolean contains(String ruleId) {
        return ruleId != null && cache.containsKey(ruleId);
    }

    /**
     * Removes the cached rule base for the given rule identifier, if present.
     *
     * @param ruleId the cache key to remove
     */
    @Override
    public void remove(String ruleId) {
        if (ruleId != null) {
            cache.remove(ruleId);
        }
    }

    /**
     * Clears all cached rule bases.
     */
    @Override
    public void clear() {
        cache.clear();
    }
}
