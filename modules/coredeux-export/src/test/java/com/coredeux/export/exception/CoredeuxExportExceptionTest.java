package com.coredeux.export.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class CoredeuxExportExceptionTest {

    @Test
    void shouldExposeMessage() {
        CoredeuxExportException exception = new CoredeuxExportException("Export failed");

        assertEquals("Export failed", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldPreserveCause() {
        IllegalStateException cause = new IllegalStateException("bad state");

        CoredeuxExportException exception = new CoredeuxExportException("Export failed", cause);

        assertEquals("Export failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
