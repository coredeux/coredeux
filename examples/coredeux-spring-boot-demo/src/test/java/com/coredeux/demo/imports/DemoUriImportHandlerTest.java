package com.coredeux.demo.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.handler.ImportValueContext;

class DemoUriImportHandlerTest {

    private final DemoUriImportHandler handler = new DemoUriImportHandler();

    @Test
    void shouldParseAbsoluteUrisAndIgnoreBlankValues() {
        final ImportValueContext absoluteContext = context("https://example.com");
        assertEquals(java.net.URI.create("https://example.com"), handler.handle(absoluteContext));

        final ImportValueContext blankContext = context(" ");
        assertNull(handler.handle(blankContext));
    }

    @Test
    void shouldRejectRelativeAndMalformedUris() {
        final ImportValueContext relativeContext = context("/relative");
        assertThrows(CoredeuxImportException.class, () -> handler.handle(relativeContext));

        final ImportValueContext malformedContext = context("::not-a-uri::");
        assertThrows(CoredeuxImportException.class, () -> handler.handle(malformedContext));
    }

    private ImportValueContext context(String effectiveValue) {
        return ImportValueContext.builder()
                .effectiveValue(effectiveValue)
                .column(ImportColumn.builder().name("uri").build())
                .build();
    }
}
