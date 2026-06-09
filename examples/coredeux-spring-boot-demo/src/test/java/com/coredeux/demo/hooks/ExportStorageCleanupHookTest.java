package com.coredeux.demo.hooks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.demo.export.ExportStorageRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

class ExportStorageCleanupHookTest {

    private final ExportStorageCleanupHook hook = new ExportStorageCleanupHook(new ObjectMapper());

    @Test
    void shouldDeleteFilesystemArtifactsWhenMetadataPointsToFile() throws Exception {
        Path file = Files.createTempFile("coredeux-export", ".txt");
        ExportStorageRecord record = ExportStorageRecord.builder()
                .storageType("filesystem")
                .metadataJson(new ObjectMapper().writeValueAsString(
                        Map.of("destination", file.toAbsolutePath().toString())))
                .build();

        assertDoesNotThrow(() -> hook.beforeDelete(record, null, null));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(file));
    }

    @Test
    void shouldFallBackToCanonicalUrlWhenMetadataIsInvalid() throws Exception {
        Path file = Files.createTempFile("coredeux-export", ".txt");
        ExportStorageRecord record = ExportStorageRecord.builder()
                .storageType("filesystem")
                .metadataJson("{not-json")
                .canonicalUrl(file.toUri().toString())
                .build();

        assertDoesNotThrow(() -> hook.beforeDelete(record, null, null));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(file));
    }

    @Test
    void shouldIgnoreMissingFilesystemTargets() throws Exception {
        Path missing = Files.createTempFile("coredeux-export", ".txt");
        Files.deleteIfExists(missing);
        ExportStorageRecord record = ExportStorageRecord.builder()
                .storageType("filesystem")
                .metadataJson(new ObjectMapper().writeValueAsString(
                        Map.of("destination", missing.toAbsolutePath().toString())))
                .build();

        assertDoesNotThrow(() -> hook.beforeDelete(record, null, null));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(missing));
    }

    @Test
    void shouldIgnoreNonFilesystemRecordsAndBlankEntities() {
        ExportStorageRecord record = ExportStorageRecord.builder().storageType("database").build();
        assertDoesNotThrow(() -> hook.beforeDelete(record, null, null));
        assertDoesNotThrow(() -> hook.beforeDelete(null, null, null));
    }

    @Test
    void shouldIgnoreFilesystemRecordsWithoutAnyPathHints() {
        ExportStorageRecord record = ExportStorageRecord.builder()
                .storageType("filesystem")
                .metadataJson("  ")
                .build();

        assertDoesNotThrow(() -> hook.beforeDelete(record, null, null));
    }

    @Test
    void shouldRejectInvalidFileUris() {
        ExportStorageRecord record = ExportStorageRecord.builder()
                .storageType("filesystem")
                .canonicalUrl("file:bad uri")
                .build();

        assertThrows(IllegalStateException.class, () -> hook.beforeDelete(record, null, null));
    }
}
