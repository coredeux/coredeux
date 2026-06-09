package com.coredeux.demo.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.handler.ImportValueContext;
import com.coredeux.impex.model.ImportColumn;

class LegacyDateImportHandlerTest {

    private final LegacyDateImportHandler handler = new LegacyDateImportHandler();

    @Test
    void shouldParseDateUsingColumnDateFormat() {
        Object value = handler.handle(context("04/05/2026", Map.of("dateFormat", "dd/MM/yyyy")));

        assertEquals(Date.class, value.getClass());
    }

    @Test
    void shouldReturnNullForBlankValue() {
        assertNull(handler.handle(context(" ", Map.of("dateFormat", "dd/MM/yyyy"))));
    }

    @Test
    void shouldRejectMissingDateFormatMetadata() {
        CoredeuxImportException exception = assertThrows(CoredeuxImportException.class,
                () -> handler.handle(context("2026-05-04", Map.of())));

        assertEquals("legacySignupDate", exception.getColumn());
    }

    @Test
    void shouldRejectInvalidValueWithColumnContext() {
        CoredeuxImportException exception = assertThrows(CoredeuxImportException.class,
                () -> handler.handle(context("not-a-date", Map.of("dateFormat", "dd/MM/yyyy"))));

        assertEquals("legacySignupDate", exception.getColumn());
    }

    @Test
    void shouldApplyTimezoneMetadataWhenPresent() {
        Object value = handler.handle(context("2026-05-04 10:15:00", Map.of(
                "dateFormat", "yyyy-MM-dd HH:mm:ss",
                "timezone", "UTC")));

        assertEquals(Date.class, value.getClass());
    }

    @Test
    void shouldRejectUnexpectedTargetType() {
        CoredeuxImportException exception = assertThrows(CoredeuxImportException.class,
                () -> handler.handle(ImportValueContext.builder()
                        .effectiveValue("2026-05-04")
                        .expectedType(String.class)
                        .column(com.coredeux.impex.model.ImportColumn.builder()
                                .name("legacySignupDate")
                                .metadata(Map.of("dateFormat", "yyyy-MM-dd"))
                                .build())
                        .build()));

        assertEquals("legacySignupDate", exception.getColumn());
    }

    private ImportValueContext context(String value, Map<String, Object> metadata) {
        return ImportValueContext.builder()
                .effectiveValue(value)
                .expectedType(Date.class)
                .column(ImportColumn.builder().name("legacySignupDate").metadata(metadata).build())
                .build();
    }
}
