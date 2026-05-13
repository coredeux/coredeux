package com.coredeux.demo.hooks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.coredeux.demo.export.ExportStorageRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

class ExportStorageCleanupHookTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deletesFilesystemArtifactBeforeDelete() throws Exception {
        Path exportFile = Files.writeString(tempDir.resolve("export.txt"), "payload");
        ExportStorageRecord record = ExportStorageRecord.builder()
                .uid("uid-1")
                .fileName("export.txt")
                .storageType("filesystem")
                .contentType("text/plain")
                .size((long) "payload".length())
                .metadataJson(objectMapper.writeValueAsString(java.util.Map.of("destination", exportFile.toString())))
                .build();

        new ExportStorageCleanupHook(objectMapper).beforeDelete(record, null, null);

        assertFalse(Files.exists(exportFile));
    }

    @Test
    void ignoresDatabaseBackedArtifacts() throws Exception {
        Path exportFile = Files.writeString(tempDir.resolve("db-export.txt"), "payload");
        ExportStorageRecord record = ExportStorageRecord.builder()
                .uid("uid-2")
                .fileName("db-export.txt")
                .storageType("database")
                .contentType("text/plain")
                .size((long) "payload".length())
                .metadataJson(objectMapper.writeValueAsString(java.util.Map.of("destination", exportFile.toString())))
                .build();

        new ExportStorageCleanupHook(objectMapper).beforeDelete(record, null, null);

        assertTrue(Files.exists(exportFile));
    }
}
