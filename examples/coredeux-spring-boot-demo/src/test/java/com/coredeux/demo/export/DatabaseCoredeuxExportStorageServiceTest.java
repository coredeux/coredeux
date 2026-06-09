package com.coredeux.demo.export;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportStorageArtifact;
import com.coredeux.export.model.ExportStorageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

class DatabaseCoredeuxExportStorageServiceTest {

    @Test
    void shouldStoreArtifactAndPersistRecord() throws Exception {
        ExportStorageRecordRepository repository = mock(ExportStorageRecordRepository.class);
        DatabaseCoredeuxExportStorageService service = new DatabaseCoredeuxExportStorageService(repository,
                new ObjectMapper());
        Path sourceFile = Files.createTempFile("coredeux-export", ".txt");
        Files.writeString(sourceFile, "hello world");

        ExportStorageRequest request = ExportStorageRequest.builder()
                .uid("job-1")
                .fileName("sample.txt")
                .contentType("text/plain")
                .format(ExportFormat.TEXT)
                .sourceFile(sourceFile)
                .build();

        ExportStorageArtifact artifact = service.store(request);

        assertEquals("database", artifact.getStorageType());
        assertEquals("sample.txt", artifact.getFileName());
        verify(repository).save(org.mockito.ArgumentMatchers.any(ExportStorageRecord.class));
    }

    @Test
    void shouldRejectInvalidRequests() {
        DatabaseCoredeuxExportStorageService service = new DatabaseCoredeuxExportStorageService(mock(ExportStorageRecordRepository.class),
                new ObjectMapper());
        assertThrows(CoredeuxExportException.class, () -> service.store(null));
        assertThrows(CoredeuxExportException.class,
                () -> service.store(ExportStorageRequest.builder().fileName("x").build()));
        assertThrows(CoredeuxExportException.class,
                () -> service.store(ExportStorageRequest.builder().sourceFile(Path.of("missing.txt")).build()));
    }

    @Test
    void shouldExposeFindByUidAndUidFallback() {
        ExportStorageRecordRepository repository = mock(ExportStorageRecordRepository.class);
        DatabaseCoredeuxExportStorageService service = new DatabaseCoredeuxExportStorageService(repository,
                new ObjectMapper());
        when(repository.findByUid("job-2")).thenReturn(Optional.of(ExportStorageRecord.builder().uid("job-2").build()));

        assertEquals("job-2", service.findByUid("job-2").orElseThrow().getUid());
        assertFalse(service.findByUid("missing").isPresent());
        assertDoesNotThrow(() -> service.findByUid("job-2"));
    }
}
