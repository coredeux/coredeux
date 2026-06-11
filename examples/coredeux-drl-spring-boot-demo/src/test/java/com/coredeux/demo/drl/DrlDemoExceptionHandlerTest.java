package com.coredeux.demo.drl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.drl.converter.DrlConversionException;

class DrlDemoExceptionHandlerTest {

    @Test
    void mapsTheDemoExceptionsToSimplePayloads() {
        DrlDemoExceptionHandler handler = new DrlDemoExceptionHandler();

        Map<String, Object> validation = handler.handleValidationException(new CoredeuxValidationException("boom"));
        assertEquals("boom", validation.get("message"));

        Map<String, Object> conversion = handler.handleConversionException(new DrlConversionException("convert boom"));
        assertEquals("convert boom", conversion.get("message"));

        Map<String, Object> notFound = handler.handleNotFound(new DrlRuleNotFoundException("missing"));
        assertEquals("No DRL rule found for code: missing", notFound.get("message"));
    }
}
