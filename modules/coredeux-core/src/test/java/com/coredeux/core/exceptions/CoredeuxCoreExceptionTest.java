package com.coredeux.core.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class CoredeuxCoreExceptionTest {

    @Test
    void shouldCreateExceptionWithNoArguments() {
        CoredeuxCoreException exception = new CoredeuxCoreException();

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "core failure";

        CoredeuxCoreException exception = new CoredeuxCoreException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "core failure";
        Throwable cause = new IllegalStateException("boom");

        CoredeuxCoreException exception = new CoredeuxCoreException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldCreateExceptionWithCause() {
        Throwable cause = new IllegalStateException("boom");

        CoredeuxCoreException exception = new CoredeuxCoreException(cause);

        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
