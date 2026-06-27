package com.coredeux.drl.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.coredeux.drl.exceptions.CoredeuxDRLException;
import com.coredeux.drl.model.RuleContext;

class RuleContextExecutionSupportTest {

    @Test
    void shouldReturnOutputWhenNoExceptionIsPresent() {
        RuleContext<String> context = RuleContext.method("sample");
        context.setOutput("ok");

        assertEquals("ok", RuleContextExecutionSupport.outputOrThrow(context, "sample rule"));
    }

    @Test
    void shouldReturnNullWhenContextIsNull() {
        assertNull(RuleContextExecutionSupport.outputOrThrow(null, "sample rule"));
    }

    @Test
    void shouldThrowWhenContextCarriesAnException() {
        RuleContext<String> context = RuleContext.method("sample");
        context.setException(new IllegalStateException("boom"));

        assertThrows(CoredeuxDRLException.class,
                () -> RuleContextExecutionSupport.outputOrThrow(context, "sample rule"));
    }

    @Test
    void shouldThrowWhenThrowIfExceptionSeesAnException() {
        RuleContext<Void> context = RuleContext.method("sample");
        context.setException(new IllegalStateException("boom"));

        assertThrows(CoredeuxDRLException.class,
                () -> RuleContextExecutionSupport.throwIfException(context, "sample rule"));
    }

    @Test
    void shouldDoNothingWhenThrowIfExceptionSeesNoException() {
        RuleContext<Void> context = RuleContext.method("sample");

        RuleContextExecutionSupport.throwIfException(context, "sample rule");
    }
}
