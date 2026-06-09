package com.coredeux.export.storage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportStorageArtifact;
import com.coredeux.export.model.ExportStorageRequest;

class DefaultCoredeuxFileSystemExportStorageServiceTest {

    @Test
    void storesExportFileAndPopulatesMetadata() throws Exception {
        Path baseDirectory = Files.createTempDirectory("coredeux-export-storage-test");
        Path sourceFile = Files.createTempFile("coredeux-export-source", ".txt");
        Files.writeString(sourceFile, "hello export", StandardCharsets.UTF_8);

        DefaultCoredeuxFileSystemExportStorageService service =
                new DefaultCoredeuxFileSystemExportStorageService(baseDirectory.toString());
        ExportStorageArtifact artifact = service.store(ExportStorageRequest.builder()
                .uid("uid-1")
                .sourceFile(sourceFile)
                .fileName("demo.txt")
                .contentType("text/plain")
                .format(ExportFormat.TEXT)
                .build());

        assertEquals("filesystem", artifact.getStorageType());
        assertEquals("demo.txt", artifact.getFileName());
        assertTrue(Files.exists(Path.of(artifact.getAbsolutePath())));
        assertEquals(Map.of(
                "baseDirectory", baseDirectory.toAbsolutePath().toString(),
                "destination", Path.of(artifact.getAbsolutePath()).toAbsolutePath().toString(),
                "size", Files.size(Path.of(artifact.getAbsolutePath())),
                "format", ExportFormat.TEXT.name(),
                "uid", "uid-1"), artifact.getMetadata());
    }

    @Test
    void rejectsMissingSourceFile() {
        DefaultCoredeuxFileSystemExportStorageService service = new DefaultCoredeuxFileSystemExportStorageService();

        assertThrows(CoredeuxExportException.class, () -> service.store(null));
        assertThrows(CoredeuxExportException.class,
                () -> service.store(ExportStorageRequest.builder().fileName("demo.txt").build()));
    }
}
