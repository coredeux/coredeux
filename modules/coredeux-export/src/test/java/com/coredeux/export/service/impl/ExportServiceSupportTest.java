package com.coredeux.export.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.handler.ExportValueHandlerResolver;
import com.coredeux.export.handler.impl.DefaultCoredeuxExportValueHandler;
import com.coredeux.export.model.ExportField;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportRequest;

class ExportServiceSupportTest {

    @Test
    void parserRejectsMissingBlankAndMalformedPaths() {
        ExportFieldPathParser parser = new ExportFieldPathParser();

        assertThrows(CoredeuxExportException.class, () -> parser.parse(null));
        assertThrows(CoredeuxExportException.class, () -> parser.parse(List.of()));
        assertThrows(CoredeuxExportException.class, () -> parser.parse(List.of(ExportField.builder().build())));
        assertThrows(CoredeuxExportException.class,
                () -> parser.parse(List.of(ExportField.builder().path("profile: ").build())));
    }

    @Test
    void parserTrimsAndSplitsEscapedSegments() {
        ExportFieldPathParser parser = new ExportFieldPathParser();

        ExportFieldPath path = parser.parse(List.of(ExportField.builder().path(" profile\\:name: value\\ ").build()))
                .get(0);

        assertEquals("profile\\:name: value\\", path.expression());
        assertEquals(List.of("profile:name", "value\\"), path.segments());
    }

    @Test
    void formatterHandlesAllSupportedValueTypes() {
        ExportValueFormatter formatter = new ExportValueFormatter();

        assertEquals("", formatter.format(null));
        assertEquals("19.90", formatter.format(new BigDecimal("19.90")));
        assertEquals("2026-05-08T00:00:00Z",
                formatter.format(java.util.Date.from(Instant.parse("2026-05-08T00:00:00Z"))));
        assertEquals("2026-05-08", formatter.format(LocalDate.parse("2026-05-08")));
        assertEquals("XLSX", formatter.format(ExportFormat.XLSX));
        assertEquals("42", formatter.format(42));
    }

    @Test
    void resolverHandlesMapsNullsCollectionsAndErrors() {
        ExportValueResolver resolver = resolver();
        ExportRequest request = ExportRequest.builder().entity("Product").build();

        assertEquals("", resolver.resolve(null, Product.class, path("name"), null, request));
        assertEquals("from-map", resolver.resolve(Map.of("metadata", Map.of("name", "from-map")), Map.class,
                path("metadata:name"), null, request));

        Product product = new Product();
        product.name = "Demo";
        product.tags = List.of("blue", "", "green");
        product.children = List.of(new Child("one"), new Child("two"));
        product.groups = List.of(new Group(List.of("nested")));
        product.attributes.put("code", "A-1");

        assertEquals("Demo", resolver.resolve(product, Product.class, path("name"), null, request));
        assertEquals("blue|green", resolver.resolve(product, Product.class, path("tags"), "|", request));
        assertEquals("one, two", resolver.resolve(product, Product.class, path("children:label"), null, request));
        assertEquals("A-1", resolver.resolve(product, Product.class, path("attributes:code"), null, request));

        assertThrows(CoredeuxExportException.class,
                () -> resolver.resolve(product, Product.class, path("missing"), null, request));
        assertThrows(CoredeuxExportException.class,
                () -> resolver.resolve(product, Product.class, path("groups:children"), null, request));
    }

    private ExportValueResolver resolver() {
        return new ExportValueResolver(new DefaultCoredeuxReflectionHelperService(), new ExportValueFormatter(),
                new ExportValueHandlerResolver(Map.of("defaultCoredeuxExportValueHandler",
                        new DefaultCoredeuxExportValueHandler())));
    }

    private ExportFieldPath path(String value) {
        return new ExportFieldPathParser().parse(List.of(ExportField.builder().path(value).build())).get(0);
    }

    static class Product {
        private String name;
        private List<String> tags;
        private List<Child> children;
        private List<Group> groups;
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    record Child(String label) {
    }

    record Group(List<String> children) {
    }
}
