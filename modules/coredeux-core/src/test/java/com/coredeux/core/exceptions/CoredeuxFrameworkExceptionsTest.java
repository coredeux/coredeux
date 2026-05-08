package com.coredeux.core.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.validation.ValidationError;

class CoredeuxFrameworkExceptionsTest {

    @Test
    void shouldCreateDataAccessExceptionWithNoArguments() {
        CoredeuxDataAccessException exception = new CoredeuxDataAccessException();

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateDataAccessExceptionWithMessage() {
        CoredeuxDataAccessException exception = new CoredeuxDataAccessException("data access failure");

        assertEquals("data access failure", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateDataAccessExceptionWithMessageAndCause() {
        Throwable cause = new IllegalStateException("db down");

        CoredeuxDataAccessException exception = new CoredeuxDataAccessException("data access failure", cause);

        assertEquals("data access failure", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldCreateDataAccessExceptionWithCause() {
        Throwable cause = new IllegalStateException("db down");

        CoredeuxDataAccessException exception = new CoredeuxDataAccessException(cause);

        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldCreateStrategyExceptionWithNoArguments() {
        CoredeuxStrategyException exception = new CoredeuxStrategyException();

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateStrategyExceptionWithMessage() {
        CoredeuxStrategyException exception = new CoredeuxStrategyException("strategy failure");

        assertEquals("strategy failure", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldCreateStrategyExceptionWithMessageAndCause() {
        Throwable cause = new IllegalArgumentException("bad phase");

        CoredeuxStrategyException exception = new CoredeuxStrategyException("strategy failure", cause);

        assertEquals("strategy failure", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldCreateStrategyExceptionWithCause() {
        Throwable cause = new IllegalArgumentException("bad phase");

        CoredeuxStrategyException exception = new CoredeuxStrategyException(cause);

        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldCreateValidationExceptionWithNoArguments() {
        CoredeuxValidationException exception = new CoredeuxValidationException();

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception.getValidationErrors().isEmpty());
    }

    @Test
    void shouldCreateValidationExceptionWithMessage() {
        CoredeuxValidationException exception = new CoredeuxValidationException("validation failure");

        assertEquals("validation failure", exception.getMessage());
        assertNull(exception.getCause());
        assertTrue(exception.getValidationErrors().isEmpty());
    }

    @Test
    void shouldCreateValidationExceptionWithMessageAndCause() {
        Throwable cause = new IllegalStateException("bad payload");

        CoredeuxValidationException exception = new CoredeuxValidationException("validation failure", cause);

        assertEquals("validation failure", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertTrue(exception.getValidationErrors().isEmpty());
    }

    @Test
    void shouldCreateValidationExceptionWithCause() {
        Throwable cause = new IllegalStateException("bad payload");

        CoredeuxValidationException exception = new CoredeuxValidationException(cause);

        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
        assertTrue(exception.getValidationErrors().isEmpty());
    }

    @Test
    void shouldCreateValidationExceptionWithCopiedValidationErrors() {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(ValidationError.builder().field("name").message("required").build());

        CoredeuxValidationException exception = new CoredeuxValidationException("validation failure", errors);
        errors.add(ValidationError.builder().field("code").message("invalid").build());

        assertEquals("validation failure", exception.getMessage());
        assertEquals(1, exception.getValidationErrors().size());
        assertEquals("name", exception.getValidationErrors().get(0).getField());
    }

    @Test
    void shouldCreateValidationExceptionWithEmptyErrorsWhenNullListProvided() {
        CoredeuxValidationException exception = new CoredeuxValidationException("validation failure", (List<ValidationError>) null);

        assertEquals("validation failure", exception.getMessage());
        assertTrue(exception.getValidationErrors().isEmpty());
    }
}

