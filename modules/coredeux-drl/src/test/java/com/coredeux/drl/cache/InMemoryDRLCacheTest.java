package com.coredeux.drl.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;

class InMemoryDRLCacheTest {

    private final InMemoryDRLCache cache = new InMemoryDRLCache();

    @Test
    void shouldStoreLookupComputeAndRemoveRules() {
        CompiledDRLRule compiledRule = new CompiledDRLRule(mock(KieBase.class), true);
        AtomicInteger supplierCalls = new AtomicInteger();

        assertTrue(cache.get("missing").isEmpty());
        assertTrue(cache.get(" ").isEmpty());
        assertFalse(cache.contains("missing"));

        cache.put("rule-1", compiledRule);
        assertTrue(cache.contains("rule-1"));
        assertEquals(compiledRule, cache.get("rule-1").orElseThrow());

        CompiledDRLRule computed = cache.computeIfAbsent("rule-2", () -> {
            supplierCalls.incrementAndGet();
            return new CompiledDRLRule(mock(KieBase.class), false);
        });
        assertNotNull(computed);
        assertEquals(1, supplierCalls.get());
        assertEquals(computed, cache.computeIfAbsent("rule-2", () -> {
            supplierCalls.incrementAndGet();
            return new CompiledDRLRule(mock(KieBase.class), true);
        }));
        assertEquals(1, supplierCalls.get());

        cache.remove("rule-1");
        assertFalse(cache.contains("rule-1"));
        cache.remove(null);
        cache.clear();
        assertFalse(cache.contains("rule-2"));
    }

    @Test
    void shouldRejectInvalidArgumentsAndNullSuppliers() {
        CompiledDRLRule compiledRule = new CompiledDRLRule(mock(KieBase.class), false);

        assertThrows(IllegalArgumentException.class, () -> cache.put(" ", compiledRule));
        assertThrows(IllegalArgumentException.class, () -> cache.put("rule", null));
        assertThrows(IllegalArgumentException.class, () -> cache.computeIfAbsent(" ", () -> compiledRule));
        assertThrows(IllegalArgumentException.class, () -> cache.computeIfAbsent("rule", null));
        assertThrows(IllegalStateException.class, () -> cache.computeIfAbsent("rule", () -> null));
    }
}
