package com.coredeux.drl.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import com.coredeux.drl.exceptions.CoredeuxDRLException;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DefaultDRLServiceInvokeTest {

    private static final String RULE_ID = "sample.invoke.drl";
    private static final String SOURCE = """
            import com.coredeux.drl.model.RuleContext;
            import com.coredeux.drl.service.impl.DemoService;

            rule "greeting"
            when
                $context : RuleContext(method == "greet")
                $service : DemoService()
            then
                $context.setOutput($context.getParams().get("prefix") + $service.message());
            end

            rule "noOutput"
            when
                $context : RuleContext(method == "noOutput")
            then
                $context.setMessage("done");
            end

            rule "fail"
            when
                $context : RuleContext(method == "fail")
            then
                $context.setException(new IllegalStateException("rule failed"));
            end
            """;

    private final DRLService service = new DefaultDRLService(ruleId -> {
        assertEquals(RULE_ID, ruleId);
        return SOURCE;
    });

    @Test
    void invokesTheContextMethodWithFactsAndParamsUsingTheStoredRuleId() {
        String result = service.invoke("  sample.invoke.drl # greet  ",
                List.of(new DemoService()), Map.of("prefix", "status: "));

        assertEquals("status: ok", result);
        assertTrue(service.isCached(RULE_ID));
    }

    @Test
    void allowsNoInputsAndNoOutput() {
        assertNull(service.invoke(RULE_ID + "#noOutput", null, null));
    }

    @Test
    void propagatesTheRuleFailureWithItsCause() {
        CoredeuxDRLException failure = assertThrows(CoredeuxDRLException.class,
                () -> service.invoke(RULE_ID + "#fail", List.of(), Map.of()));

        assertEquals("rule failed", failure.getCause().getMessage());
    }

    @Test
    void surfacesAContextFailureFromAnImplementationThatReturnsNormally() {
        DRLService contextBasedService = mock(DRLService.class, CALLS_REAL_METHODS);
        IllegalStateException cause = new IllegalStateException("captured failure");
        doAnswer(invocation -> {
            RuleContext<?> context = invocation.getArgument(1);
            context.setException(cause);
            return null;
        }).when(contextBasedService).execute(eq(RULE_ID), any());

        CoredeuxDRLException failure = assertThrows(CoredeuxDRLException.class,
                () -> contextBasedService.invoke(RULE_ID + "#fail", List.of(), Map.of()));

        assertEquals(cause, failure.getCause());
    }

    @Test
    void propagatesTheNoMatchingRuleFailure() {
        CoredeuxDRLException failure = assertThrows(CoredeuxDRLException.class,
                () -> service.invoke(RULE_ID + "#unknown", List.of(), Map.of()));

        assertTrue(failure.getMessage().contains("unknown"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "sample.invoke.drl", "sample.invoke.drl#", " #greet",
            "sample.invoke.drl#   ", "sample.invoke.drl#greet#extra"})
    void rejectsMalformedTargetsBeforeResolvingTheRule(String target) {
        assertThrows(IllegalArgumentException.class, () -> service.invoke(target, List.of(), Map.of()));
        assertFalse(service.isCached(RULE_ID));
    }
}
