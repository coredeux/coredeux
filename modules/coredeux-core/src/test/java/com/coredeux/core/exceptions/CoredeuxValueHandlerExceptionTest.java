package com.coredeux.core.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class CoredeuxValueHandlerExceptionTest {

    @Test
    void shouldCreateExceptionWithNoArguments() {
        CoredeuxValueHandlerException exception = new CoredeuxValueHandlerException();

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithMessage() {
        CoredeuxValueHandlerException exception = new CoredeuxValueHandlerException("value handler failure");

        assertEquals("value handler failure", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        Throwable cause = new IllegalStateException("boom");

        CoredeuxValueHandlerException exception = new CoredeuxValueHandlerException("value handler failure", cause);

        assertEquals("value handler failure", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithCause() {
        Throwable cause = new IllegalStateException("boom");

        CoredeuxValueHandlerException exception = new CoredeuxValueHandlerException(cause);

        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
