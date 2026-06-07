package com.coredeux.drl.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.kie.api.KieBase;

public class InMemoryDRLCache implements DRLCache {

    private final Map<String, KieBase> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<KieBase> get(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(ruleId));
    }

    @Override
    public void put(String ruleId, KieBase kieBase) {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }
        if (kieBase == null) {
            throw new IllegalArgumentException("kieBase is required for ruleId: " + ruleId);
        }
        cache.put(ruleId, kieBase);
    }

    @Override
    public KieBase computeIfAbsent(String ruleId, Supplier<KieBase> loader) {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }
        if (loader == null) {
            throw new IllegalArgumentException("loader is required for ruleId: " + ruleId);
        }
        return cache.computeIfAbsent(ruleId, key -> {
            KieBase kieBase = loader.get();
            if (kieBase == null) {
                throw new IllegalStateException("Loader returned null KieBase for ruleId: " + key);
            }
            return kieBase;
        });
    }

    @Override
    public boolean contains(String ruleId) {
        return ruleId != null && cache.containsKey(ruleId);
    }

    @Override
    public void remove(String ruleId) {
        if (ruleId != null) {
            cache.remove(ruleId);
        }
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
