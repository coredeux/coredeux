package com.coredeux.export.log.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.model.ExportLogEntry;

class ExportLogServiceTest {

    @TempDir
    Path tempDir;

    private String originalTmpDir;

    @AfterEach
    void restoreTmpDir() {
        if (originalTmpDir != null) {
            System.setProperty("java.io.tmpdir", originalTmpDir);
        }
    }

    @Test
    void shouldWriteAndReadFileLogs() {
        FileCoredeuxExportLogService logs = fileLogs();

        assertTrue(logs.findByUid("job-1").isEmpty());

        logs.info("job-1", "started", Map.of("page", 1));
        logs.warn("job-1", "slow", null);
        logs.error("job-1", "failed", new IllegalStateException("boom"), Map.of("retry", true));

        List<ExportLogEntry> entries = logs.findByUid("job-1");
        assertEquals(3, entries.size());
        assertEquals("INFO", entries.get(0).getLevel());
        assertEquals("started", entries.get(0).getMessage());
        assertEquals(1, entries.get(0).getMetadata().get("page"));
        assertEquals("WARN", entries.get(1).getLevel());
        assertTrue(entries.get(1).getMetadata().isEmpty());
        assertEquals("ERROR", entries.get(2).getLevel());
        assertEquals(IllegalStateException.class.getName(), entries.get(2).getExceptionType());
        assertTrue(entries.get(2).getStackTrace().contains("IllegalStateException: boom"));
    }

    @Test
    void shouldResolveConfiguredDefaultNamedAndSingleFallbackServices() {
        CoredeuxExportLogService defaultService = new ConsoleCoredeuxExportLogService();
        CoredeuxExportLogService namedService = new ConsoleCoredeuxExportLogService();
        DefaultCoredeuxExportLogServiceResolver resolver = new DefaultCoredeuxExportLogServiceResolver(Map.of(
                "defaultLogs", defaultService,
                "namedLogs", namedService), "defaultLogs");

        assertSame(defaultService, resolver.resolve(null));
        assertSame(defaultService, resolver.resolve(" "));
        assertSame(namedService, resolver.resolve(" namedLogs "));

        DefaultCoredeuxExportLogServiceResolver single = new DefaultCoredeuxExportLogServiceResolver(Map.of(
                "only", namedService), "missingDefault");
        assertSame(namedService, single.resolve("missing"));
    }

    @Test
    void shouldRejectMissingLogServiceWhenMultipleServicesExist() {
        DefaultCoredeuxExportLogServiceResolver resolver = new DefaultCoredeuxExportLogServiceResolver(Map.of(
                "a", new ConsoleCoredeuxExportLogService(),
                "b", new ConsoleCoredeuxExportLogService()), "a");

        CoredeuxExportException exception = assertThrows(CoredeuxExportException.class,
                () -> resolver.resolve("missing"));

        assertTrue(exception.getMessage().contains("Unable to resolve export log service: missing"));
    }

    @Test
    void shouldLogToConsoleAndReturnNoStoredEntries() {
        ConsoleCoredeuxExportLogService logs = new ConsoleCoredeuxExportLogService();

        logs.info("job-2", "info", null);
        logs.warn("job-2", "warn", Map.of("key", "value"));
        logs.error("job-2", "error", null, null);

        assertFalse(logs.findByUid("job-2").iterator().hasNext());
    }

    private FileCoredeuxExportLogService fileLogs() {
        originalTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", tempDir.toString());
        return new FileCoredeuxExportLogService();
    }
}
