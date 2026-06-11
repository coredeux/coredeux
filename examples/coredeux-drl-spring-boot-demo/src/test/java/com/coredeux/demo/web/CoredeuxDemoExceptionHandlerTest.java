package com.coredeux.demo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxValidationException;

class CoredeuxDemoExceptionHandlerTest {

    @Test
    void mapsValidationExceptionsToJson() {
        CoredeuxDemoExceptionHandler handler = new CoredeuxDemoExceptionHandler();
        CoredeuxValidationException exception = new CoredeuxValidationException("boom");

        Map<String, Object> response = handler.handleValidationException(exception);

        assertEquals("boom", response.get("message"));
    }
}
