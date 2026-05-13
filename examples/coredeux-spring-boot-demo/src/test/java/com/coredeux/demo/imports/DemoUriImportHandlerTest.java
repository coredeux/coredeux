package com.coredeux.demo.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.handler.ImportValueContext;
import com.coredeux.impex.model.ImportColumn;

class DemoUriImportHandlerTest {

    private final DemoUriImportHandler handler = new DemoUriImportHandler();

    @Test
    void shouldConvertAbsoluteUri() {
        Object value = handler.handle(context(" https://docs.coredeux.dev/products/import "));

        assertEquals(URI.create("https://docs.coredeux.dev/products/import"), value);
    }

    @Test
    void shouldReturnNullForBlankValue() {
        assertNull(handler.handle(context(" ")));
    }

    @Test
    void shouldRejectRelativeUriWithColumnContext() {
        CoredeuxImportException exception = assertThrows(CoredeuxImportException.class,
                () -> handler.handle(context("/relative/path")));

        assertEquals("documentationUrl", exception.getColumn());
    }

    private ImportValueContext context(String value) {
        return ImportValueContext.builder()
                .effectiveValue(value)
                .column(ImportColumn.builder().name("documentationUrl").build())
                .build();
    }
}
