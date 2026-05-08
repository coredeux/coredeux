package com.coredeux.export.queue.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportField;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportJob;
import com.coredeux.export.model.ExportOptions;
import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;
import com.coredeux.export.model.ExportStatus;

class FileCoredeuxExportQueueServiceTest {

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
    void shouldEnqueueClaimUpdateCompleteAndFindJobs() {
        FileCoredeuxExportQueueService queue = queueService();
        ExportRequest request = request();

        ExportJob job = queue.enqueue(request);

        assertNotNull(job.getUid());
        assertEquals(ExportStatus.NEW, job.getStatus());
        assertEquals(ExportStatus.NEW, job.getResponse().getStatus());
        assertEquals(ExportFormat.XLSX, job.getResponse().getFormat());
        assertEquals("products.xlsx", job.getResponse().getFileName());
        assertEquals(List.of("sku", "name"), job.getResponse().getFields());
        assertEquals(0, queue.countInProgress());
        assertTrue(queue.findByUid(job.getUid()).isPresent());
        assertTrue(queue.findByUid(null).isEmpty());
        assertTrue(queue.claimNext(0).isEmpty());

        List<ExportJob> claimed = queue.claimNext(1);

        assertEquals(1, claimed.size());
        assertEquals(ExportStatus.IN_PROGRESS, claimed.get(0).getStatus());
        assertNotNull(claimed.get(0).getStartedAt());
        assertEquals(1, queue.countInProgress());

        queue.updateProgress(job.getUid(), 25);
        ExportJob progress = queue.findByUid(job.getUid()).orElseThrow();
        assertEquals(25, progress.getResponse().getRowCount());
        assertEquals(ExportStatus.IN_PROGRESS, progress.getResponse().getStatus());

        ExportResponse completed = ExportResponse.builder().rowCount(25).contentType("text/plain").build();
        queue.markCompleted(job.getUid(), completed);
        ExportJob completedJob = queue.findByUid(job.getUid()).orElseThrow();
        assertEquals(ExportStatus.COMPLETED, completedJob.getStatus());
        assertEquals(ExportStatus.COMPLETED, completedJob.getResponse().getStatus());
        assertEquals(job.getUid(), completedJob.getResponse().getUid());
        assertEquals(25, completedJob.getResponse().getRowCount());
        assertNotNull(completedJob.getCompletedAt());
    }

    @Test
    void shouldMarkErrorsAndCreateResponseWhenMissing() {
        FileCoredeuxExportQueueService queue = queueService();
        ExportJob job = queue.enqueue(null);
        queue.claimNext(1);

        queue.markCompleted(job.getUid(), null);
        ExportJob completed = queue.findByUid(job.getUid()).orElseThrow();
        assertEquals(ExportStatus.COMPLETED, completed.getResponse().getStatus());

        ExportJob errorJob = queue.enqueue(null);
        queue.claimNext(1);
        queue.markError(errorJob.getUid(), "boom");

        ExportJob failed = queue.findByUid(errorJob.getUid()).orElseThrow();
        assertEquals(ExportStatus.ERROR, failed.getStatus());
        assertEquals("boom", failed.getErrorMessage());
        assertEquals("boom", failed.getResponse().getErrorMessage());
    }

    @Test
    void shouldRejectUnknownUidAndReadBlankQueueAsEmpty() throws Exception {
        FileCoredeuxExportQueueService queue = queueService();
        Path queueFile = tempDir.resolve("coredeux-export").resolve("queue").resolve("export-queue.json");
        Files.createDirectories(queueFile.getParent());
        Files.writeString(queueFile, "   ");

        assertTrue(queue.findByUid("missing").isEmpty());
        CoredeuxExportException exception = assertThrows(CoredeuxExportException.class,
                () -> queue.markError("missing", "boom"));

        assertTrue(exception.getMessage().contains("Unknown export uid: missing"));
    }

    private FileCoredeuxExportQueueService queueService() {
        originalTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", tempDir.toString());
        return new FileCoredeuxExportQueueService();
    }

    private ExportRequest request() {
        return ExportRequest.builder()
                .entity("Product")
                .fieldList(List.of(
                        ExportField.builder().path("sku").build(),
                        ExportField.builder().path("name").build()))
                .options(ExportOptions.builder()
                        .format(ExportFormat.XLSX)
                        .fileName("products.xlsx")
                        .build())
                .build();
    }
}
