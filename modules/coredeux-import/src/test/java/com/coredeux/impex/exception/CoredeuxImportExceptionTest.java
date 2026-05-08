package com.coredeux.impex.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class CoredeuxImportExceptionTest {

    @Test
    void shouldExposeMessageWithoutColumn() {
        CoredeuxImportException exception = new CoredeuxImportException("Import failed");

        assertEquals("Import failed", exception.getMessage());
        assertNull(exception.getColumn());
        assertNull(exception.getCause());
    }

    @Test
    void shouldPreserveCauseWithoutColumn() {
        IllegalStateException cause = new IllegalStateException("bad state");

        CoredeuxImportException exception = new CoredeuxImportException("Import failed", cause);

        assertEquals("Import failed", exception.getMessage());
        assertNull(exception.getColumn());
        assertSame(cause, exception.getCause());
    }

    @Test
    void shouldExposeColumnAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("bad value");

        CoredeuxImportException exception = new CoredeuxImportException("Column failed", "sku", cause);

        assertEquals("Column failed", exception.getMessage());
        assertEquals("sku", exception.getColumn());
        assertSame(cause, exception.getCause());
    }
}
