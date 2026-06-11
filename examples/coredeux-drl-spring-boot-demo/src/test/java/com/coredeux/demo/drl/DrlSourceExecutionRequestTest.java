package com.coredeux.demo.drl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.coredeux.drl.model.RuleContext;

class DrlSourceExecutionRequestTest {

    @Test
    void storesAndReturnsTheSourceAndContextPayload() {
        DrlSourceExecutionRequest request = new DrlSourceExecutionRequest();
        RuleContext<String> context = RuleContext.<String>method("execute").param("entity", "demo");

        request.setSource("rule \"demo\"");
        request.setContext(context);

        assertEquals("rule \"demo\"", request.getSource());
        assertSame(context, request.getContext());
    }
}
