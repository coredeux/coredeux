package com.coredeux.drl.cache;

import java.util.Optional;
import java.util.function.Supplier;

import org.kie.api.KieBase;

public interface DRLCache {

    Optional<KieBase> get(String ruleId);

    void put(String ruleId, KieBase kieBase);

    KieBase computeIfAbsent(String ruleId, Supplier<KieBase> loader);

    boolean contains(String ruleId);

    void remove(String ruleId);

    void clear();
}
