package com.coredeux.export.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExportModelTest {

    @Test
    void shouldInitializeBuilderDefaults() {
        ExportField field = ExportField.builder().path("sku").build();
        ExportJob job = ExportJob.builder().uid("job-1").build();
        ExportLogEntry log = ExportLogEntry.builder().uid("job-1").build();
        ExportOptions options = ExportOptions.builder().build();
        ExportQuery query = ExportQuery.builder().text("sku = :sku").build();
        ExportRequest request = ExportRequest.builder().entity("Product").build();
        ExportResponse response = ExportResponse.builder().uid("job-1").build();

        assertNotNull(field.getMetadata());
        assertNotNull(job.getMetadata());
        assertNotNull(log.getMetadata());
        assertNull(options.getFormat());
        assertEquals(100, options.getBatchSize());
        assertEquals(Boolean.TRUE, options.getIncludeHeader());
        assertEquals("|", options.getTextSeparator());
        assertEquals(", ", options.getCollectionSeparator());
        assertNotNull(query.getParams());
        assertNotNull(request.getFieldList());
        assertNotNull(request.getSearchParams());
        assertNotNull(request.getOptions());
        assertNotNull(response.getFields());
        assertNotNull(response.getLogs());
        assertNotNull(response.getMetadata());
    }

    @Test
    void shouldSupportFieldOptionsAndQueryGeneratedMethods() {
        ExportField field = new ExportField("sku", "handler", new LinkedHashMap<>(Map.of("label", "SKU")));
        ExportField sameField = ExportField.builder()
                .path("sku")
                .handler("handler")
                .metadata(new LinkedHashMap<>(Map.of("label", "SKU")))
                .build();
        ExportOptions options = new ExportOptions(ExportFormat.XLSX, 50, 25, false, "|", ";", "products.xlsx",
                "storage");
        ExportOptions sameOptions = ExportOptions.builder()
                .format(ExportFormat.XLSX)
                .limit(50)
                .batchSize(25)
                .includeHeader(false)
                .textSeparator("|")
                .collectionSeparator(";")
                .fileName("products.xlsx")
                .storageService("storage")
                .build();
        ExportQuery query = new ExportQuery("sku = :sku", new LinkedHashMap<>(Map.of("sku", "A-1")));

        assertEquals(sameField, field);
        assertEquals(sameField.hashCode(), field.hashCode());
        assertNotEquals(ExportField.builder().path("name").build(), field);
        assertTrue(field.toString().contains("sku"));
        field.setPath("name");
        field.setHandler("other");
        field.setMetadata(new LinkedHashMap<>(Map.of("label", "Name")));
        assertEquals("name", field.getPath());
        assertEquals("other", field.getHandler());
        assertEquals("Name", field.getMetadata().get("label"));

        assertEquals(sameOptions, options);
        assertEquals(sameOptions.hashCode(), options.hashCode());
        assertTrue(options.toString().contains("products.xlsx"));
        options.setLimit(10);
        options.setIncludeHeader(true);
        assertEquals(10, options.getLimit());
        assertTrue(options.getIncludeHeader());

        assertEquals(new ExportQuery("sku = :sku", new LinkedHashMap<>(Map.of("sku", "A-1"))), query);
        query.setText("name = :name");
        query.setParams(new LinkedHashMap<>(Map.of("name", "Demo")));
        assertEquals("name = :name", query.getText());
        assertEquals("Demo", query.getParams().get("name"));
    }

    @Test
    void shouldSupportJobRequestResponseAndLogGeneratedMethods() {
        Instant now = Instant.parse("2026-05-08T00:00:00Z");
        ExportField field = ExportField.builder().path("sku").build();
        ExportRequest request = new ExportRequest("Product", new ArrayList<>(List.of(field)), new ArrayList<>(),
                ExportQuery.builder().text("sku = :sku").build(), ExportOptions.builder().fileName("out.txt").build());
        ExportStorageArtifact artifact = ExportStorageArtifact.builder()
                .storageType("storage")
                .fileName("out.txt")
                .absolutePath("/tmp/out.txt")
                .relativePath("out.txt")
                .url("file:/tmp/out.txt")
                .canonicalUrl("file:/tmp/out.txt")
                .contentType("text/plain")
                .size(10L)
                .build();
        ExportLogEntry log = new ExportLogEntry("job-1", now, "INFO", "done", null, null,
                new LinkedHashMap<>(Map.of("rows", 2)));
        ExportResponse response = new ExportResponse("job-1", ExportStatus.COMPLETED, ExportFormat.TEXT, "out.txt",
                "text/plain", artifact, 2L, null, now, now, now, new ArrayList<>(List.of("sku")),
                new ArrayList<>(List.of(log)), new LinkedHashMap<>(Map.of("source", "test")));
        ExportJob job = new ExportJob("job-1", ExportStatus.COMPLETED, now, now, now, null, request, response,
                new LinkedHashMap<>(Map.of("tenant", "demo")));

        assertEquals(job, ExportJob.builder()
                .uid("job-1")
                .status(ExportStatus.COMPLETED)
                .createdAt(now)
                .startedAt(now)
                .completedAt(now)
                .request(request)
                .response(response)
                .metadata(new LinkedHashMap<>(Map.of("tenant", "demo")))
                .build());
        assertEquals(response, ExportResponse.builder()
                .uid("job-1")
                .status(ExportStatus.COMPLETED)
                .format(ExportFormat.TEXT)
                .fileName("out.txt")
                .contentType("text/plain")
                .storage(artifact)
                .rowCount(2L)
                .createdAt(now)
                .startedAt(now)
                .completedAt(now)
                .fields(new ArrayList<>(List.of("sku")))
                .logs(new ArrayList<>(List.of(log)))
                .metadata(new LinkedHashMap<>(Map.of("source", "test")))
                .build());
        assertEquals(request, ExportRequest.builder()
                .entity("Product")
                .fieldList(new ArrayList<>(List.of(field)))
                .searchParams(new ArrayList<>())
                .query(request.getQuery())
                .options(request.getOptions())
                .build());
        assertEquals(log, ExportLogEntry.builder()
                .uid("job-1")
                .timestamp(now)
                .level("INFO")
                .message("done")
                .metadata(new LinkedHashMap<>(Map.of("rows", 2)))
                .build());
        assertTrue(job.toString().contains("job-1"));
        assertFalse(job.equals(null));
        assertNotEquals(job, ExportJob.builder().uid("other").build());

        job.setErrorMessage("boom");
        response.setErrorMessage("boom");
        log.setStackTrace("stack");
        request.setEntity("Customer");
        assertEquals("boom", job.getErrorMessage());
        assertEquals("boom", response.getErrorMessage());
        assertEquals("stack", log.getStackTrace());
        assertEquals("Customer", request.getEntity());
    }
}
