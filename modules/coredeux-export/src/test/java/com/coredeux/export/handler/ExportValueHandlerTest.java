package com.coredeux.export.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.handler.impl.DefaultCoredeuxExportValueHandler;
import com.coredeux.export.model.ExportField;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportRequest;

class ExportValueHandlerTest {

    private final DefaultCoredeuxExportValueHandler defaultHandler = new DefaultCoredeuxExportValueHandler();

    @Test
    void defaultHandlerFormatsCommonValueTypes() {
        assertEquals("", defaultHandler.handle(context(null)));
        assertEquals("19.90", defaultHandler.handle(context(new BigDecimal("19.90"))));
        assertEquals("2026-05-08T00:00:00Z",
                defaultHandler.handle(context(Date.from(Instant.parse("2026-05-08T00:00:00Z")))));
        assertEquals("2026-05-08", defaultHandler.handle(context(LocalDate.parse("2026-05-08"))));
        assertEquals("XLSX", defaultHandler.handle(context(ExportFormat.XLSX)));

        Object raw = new Object();
        assertSame(raw, defaultHandler.handle(context(raw)));
    }

    @Test
    void contextBuilderExposesAllValues() {
        ExportField field = ExportField.builder().path("name").handler("handler").build();
        ExportRequest request = ExportRequest.builder().entity("Product").build();
        Object root = new Object();

        ExportValueContext context = ExportValueContext.builder()
                .rootEntity(root)
                .resolvedValue("Demo")
                .fieldPath("name")
                .entityType(String.class)
                .field(field)
                .request(request)
                .build();

        assertSame(root, context.getRootEntity());
        assertEquals("Demo", context.getResolvedValue());
        assertEquals("name", context.getFieldPath());
        assertEquals(String.class, context.getEntityType());
        assertSame(field, context.getField());
        assertSame(request, context.getRequest());
    }

    @Test
    void resolverUsesDefaultTrimmedNamedAndErrorPaths() {
        CoredeuxExportValueHandler named = value -> "named";
        CoredeuxExportValueHandler fallback = value -> "default";
        ExportValueHandlerResolver resolver = new ExportValueHandlerResolver(Map.of(
                "defaultCoredeuxExportValueHandler", fallback,
                "customHandler", named));

        assertSame(fallback, resolver.resolve(null));
        assertSame(fallback, resolver.resolve(" "));
        assertSame(named, resolver.resolve(" customHandler "));

        CoredeuxExportException exception = assertThrows(CoredeuxExportException.class,
                () -> resolver.resolve("missingHandler"));
        assertTrue(exception.getMessage().contains("missingHandler"));
    }

    private ExportValueContext context(Object value) {
        return ExportValueContext.builder()
                .resolvedValue(value)
                .fieldPath("value")
                .field(ExportField.builder().path("value").build())
                .request(ExportRequest.builder().entity("Product").build())
                .build();
    }
}
