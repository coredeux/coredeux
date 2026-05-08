package com.coredeux.demo.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportStorageArtifact;
import com.coredeux.export.model.ExportStorageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DatabaseCoredeuxExportStorageServiceTest {

    @Mock
    private ExportStorageRecordRepository repository;

    private DatabaseCoredeuxExportStorageService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new DatabaseCoredeuxExportStorageService(repository, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void shouldStoreExportFileInDatabaseAndReturnArtifact() throws Exception {
        Path source = tempDir.resolve("export.txt");
        Files.writeString(source, "hello coredeux");
        when(repository.save(any(ExportStorageRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExportStorageArtifact artifact = service.store(ExportStorageRequest.builder()
                .uid("exp-123")
                .sourceFile(source)
                .fileName("customers.txt")
                .contentType("text/plain;charset=UTF-8")
                .format(ExportFormat.TEXT)
                .build());

        assertEquals("database", artifact.getStorageType());
        assertEquals("customers.txt", artifact.getFileName());
        assertEquals("exp-123", artifact.getMetadata().get("uid"));
        assertEquals("database", artifact.getMetadata().get("storage"));
        assertTrue(artifact.getUrl().contains("/api/export/exp-123/download"));

        ArgumentCaptor<ExportStorageRecord> captor = ArgumentCaptor.forClass(ExportStorageRecord.class);
        verify(repository).save(captor.capture());
        ExportStorageRecord saved = captor.getValue();
        assertEquals("exp-123", saved.getUid());
        assertNotNull(saved.getContent());
        assertEquals("hello coredeux", new String(saved.getContent()));
    }
}
