package com.coredeux.demo.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.handler.ExportValueContext;
import com.coredeux.export.model.ExportField;

class DateFormatExportHandlerTest {

    private final DateFormatExportHandler handler = new DateFormatExportHandler();

    @Test
    void shouldFormatDateUsingMetadataPatternAndTimezone() {
        Object value = handler.handle(context(Date.from(Instant.parse("2026-05-04T13:30:00Z")),
                Map.of("dateFormat", "dd/MM/yyyy HH:mm", "timezone", "Australia/Sydney")));

        assertEquals("04/05/2026 23:30", value);
    }

    @Test
    void shouldUseUtcWhenTimezoneIsNotProvided() {
        Object value = handler.handle(context(Date.from(Instant.parse("2026-05-04T13:30:00Z")),
                Map.of("dateFormat", "yyyy-MM-dd HH:mm")));

        assertEquals("2026-05-04 13:30", value);
    }

    @Test
    void shouldReturnBlankForNullValue() {
        assertEquals("", handler.handle(context(null, Map.of("dateFormat", "dd/MM/yyyy"))));
    }

    @Test
    void shouldRejectMissingDateFormat() {
        assertThrows(CoredeuxExportException.class, () -> handler.handle(context(new Date(), Map.of())));
    }

    @Test
    void shouldRejectUnsupportedValueType() {
        assertThrows(CoredeuxExportException.class,
                () -> handler.handle(context("2026-05-04", Map.of("dateFormat", "dd/MM/yyyy"))));
    }

    @Test
    void shouldRejectInvalidTimezone() {
        assertThrows(CoredeuxExportException.class,
                () -> handler.handle(context(new Date(), Map.of("dateFormat", "dd/MM/yyyy", "timezone", "bad zone"))));
    }

    @Test
    void shouldRejectInvalidDateFormat() {
        assertThrows(CoredeuxExportException.class,
                () -> handler.handle(context(new Date(), Map.of("dateFormat", "yyyy-MM-dd 'unterminated"))));
    }

    private ExportValueContext context(Object value, Map<String, Object> metadata) {
        return ExportValueContext.builder()
                .resolvedValue(value)
                .fieldPath("profile:legacySignupDate")
                .field(ExportField.builder()
                        .path("profile:legacySignupDate")
                        .metadata(metadata)
                        .build())
                .build();
    }
}
