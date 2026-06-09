package com.coredeux.drl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RuleContextTest {

    @Test
    void shouldBuildAndMutateTheExecutionEnvelope() {
        RuleContext context = RuleContext.method("check")
                .param("entity", "original")
                .fact("first-fact")
                .fact(null);

        Map<String, Object> params = new HashMap<>();
        params.put("entity", "entity-from-map");
        context.setParams(params);
        params.put("mutated", Boolean.TRUE);

        List<Object> facts = new ArrayList<>();
        facts.add("second-fact");
        context.setFacts(facts);
        facts.add("mutated-fact");

        context.setOutput("output");
        context.setMessage("message");
        Exception exception = new IllegalStateException("boom");
        context.setException(exception);
        context.setFiredRules(3);

        assertEquals("check", context.getMethod());
        assertEquals("entity-from-map", context.getParams().get("entity"));
        assertFalse(context.getParams().containsKey("mutated"));
        assertTrue(context.getFacts().contains("second-fact"));
        assertFalse(context.getFacts().contains("mutated-fact"));
        assertEquals("output", context.getOutput());
        assertEquals("message", context.getMessage());
        assertSame(exception, context.getException());
        assertEquals(3, context.getFiredRules());

        context.getParams().put("live", "value");
        context.getFacts().add("live-fact");
        assertEquals("value", context.getParams().get("live"));
        assertTrue(context.getFacts().contains("live-fact"));

        context.setParams(null);
        context.setFacts(null);

        assertTrue(context.getParams().isEmpty());
        assertTrue(context.getFacts().isEmpty());
        assertEquals("output", context.getOutput());
    }

    @Test
    void shouldResetNullCollectionsAndExposeFluentFactInsertions() {
        RuleContext context = new RuleContext();

        context.setMethod("sample");
        context.setParams(null);
        context.setFacts(null);
        context.fact("one").fact("two");

        assertEquals("sample", context.getMethod());
        assertTrue(context.getParams().isEmpty());
        assertEquals(List.of("one", "two"), context.getFacts());
        assertFalse(context.getFacts().contains(null));

        context.param("x", null);
        assertTrue(context.getParams().containsKey("x"));
        assertNull(context.getParams().get("missing"));

        RuleContext created = RuleContext.method("created");
        assertEquals("created", created.getMethod());
        assertSame(created, created.param("k", "v"));
    }
}
