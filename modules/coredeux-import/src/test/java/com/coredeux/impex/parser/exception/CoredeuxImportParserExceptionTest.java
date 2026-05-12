package com.coredeux.impex.parser.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CoredeuxImportParserExceptionTest {

    @Test
    void shouldExposeParserFriendlyMessage() {
        CoredeuxImportParserException exception = new CoredeuxImportParserException("Invalid import file");

        assertEquals("Invalid import file", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldPreserveCauseWithParserFriendlyMessage() {
        IllegalArgumentException cause = new IllegalArgumentException("Bad cell value");

        CoredeuxImportParserException exception = new CoredeuxImportParserException("Unable to parse cell", cause);

        assertEquals("Unable to parse cell", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
