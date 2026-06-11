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

import com.coredeux.core.exceptions.CoredeuxValueHandlerException;
import com.coredeux.core.handler.service.impl.DefaultCoredeuxValueHandlerService;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
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
        DefaultCoredeuxValueHandlerService service = new DefaultCoredeuxValueHandlerService(
                InMemoryCoredeuxComponentRegistry.builder()
                        .component("defaultCoredeuxExportValueHandler", fallback)
                        .component("customHandler", named)
                        .build());

        assertEquals("default", service.invoke("defaultCoredeuxExportValueHandler", context("raw")));
        assertEquals("named", service.invoke(" customHandler ", context("raw")));

        CoredeuxValueHandlerException exception = assertThrows(CoredeuxValueHandlerException.class,
                () -> service.invoke("missingHandler", context("raw")));
        assertTrue(exception.getMessage().contains("Unable to resolve value handler: missingHandler"));
    }

    @Test
    void serviceRejectsBlankHandlerNames() {
        DefaultCoredeuxValueHandlerService service = new DefaultCoredeuxValueHandlerService(
                InMemoryCoredeuxComponentRegistry.builder().build());

        com.coredeux.core.exceptions.CoredeuxValueHandlerException exception = assertThrows(
                com.coredeux.core.exceptions.CoredeuxValueHandlerException.class,
                () -> service.invoke(" ", context("raw")));

        assertEquals("Value handler name is required", exception.getMessage());
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
