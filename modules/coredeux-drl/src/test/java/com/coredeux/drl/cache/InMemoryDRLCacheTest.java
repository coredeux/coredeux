package com.coredeux.drl.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.internal.utils.KieHelper;

class InMemoryDRLCacheTest {

    @Test
    void supportsValidationLookupAndCacheLifecycle() {
        InMemoryDRLCache cache = new InMemoryDRLCache();
        CompiledDRLRule compiledRule = compiledRule(false);

        assertTrue(cache.get(null).isEmpty());
        assertTrue(cache.get(" ").isEmpty());
        assertFalse(cache.contains(null));
        assertFalse(cache.contains(" "));

        assertThrows(IllegalArgumentException.class, () -> cache.put(null, compiledRule));
        assertThrows(IllegalArgumentException.class, () -> cache.put("rule", null));
        assertThrows(IllegalArgumentException.class, () -> cache.computeIfAbsent(null, () -> compiledRule));
        assertThrows(IllegalArgumentException.class, () -> cache.computeIfAbsent("rule", null));
        assertThrows(IllegalStateException.class, () -> cache.computeIfAbsent("rule", () -> null));
        cache.remove(null);

        AtomicInteger loaderCount = new AtomicInteger();
        CompiledDRLRule first = cache.computeIfAbsent("rule", () -> {
            loaderCount.incrementAndGet();
            return compiledRule;
        });
        CompiledDRLRule second = cache.computeIfAbsent("rule", () -> {
            loaderCount.incrementAndGet();
            return compiledRule(true);
        });

        assertSame(first, second);
        assertEquals(1, loaderCount.get());
        assertTrue(cache.contains("rule"));
        assertEquals(Optional.of(first), cache.get("rule"));

        cache.remove("rule");
        assertFalse(cache.contains("rule"));
        assertTrue(cache.get("rule").isEmpty());

        cache.put("rule", compiledRule(true));
        assertTrue(cache.contains("rule"));
        cache.clear();
        assertFalse(cache.contains("rule"));
    }

    private CompiledDRLRule compiledRule(boolean requiresComponentRegistry) {
        KieBase kieBase = new KieHelper()
                .addContent("""
                        package rules;

                        rule "cache-rule"
                        when
                        then
                        end
                        """, ResourceType.DRL)
                .build();
        return new CompiledDRLRule(kieBase, requiresComponentRegistry);
    }
}
