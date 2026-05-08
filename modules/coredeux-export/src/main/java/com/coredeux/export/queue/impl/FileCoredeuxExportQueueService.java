package com.coredeux.export.queue.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportJob;
import com.coredeux.export.model.ExportOptions;
import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;
import com.coredeux.export.model.ExportStatus;
import com.coredeux.export.queue.CoredeuxExportQueueService;
import com.coredeux.export.support.ExportJsonSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("defaultCoredeuxExportQueueService")
public class FileCoredeuxExportQueueService implements CoredeuxExportQueueService {

    private final ObjectMapper objectMapper = ExportJsonSupport.objectMapper();
    private final Path queueFile;

    public FileCoredeuxExportQueueService() {
        Path baseDirectory = Path.of(System.getProperty("java.io.tmpdir"), "coredeux-export", "queue");
        this.queueFile = baseDirectory.resolve("export-queue.json");
    }

    @Override
    public synchronized ExportJob enqueue(ExportRequest request) {
        ExportJob job = ExportJob.builder()
                .uid(UUID.randomUUID().toString())
                .status(ExportStatus.NEW)
                .createdAt(Instant.now())
                .request(request)
                .response(pendingResponse(request, ExportStatus.NEW, null))
                .build();
        List<ExportJob> jobs = loadJobs();
        jobs.add(job);
        saveJobs(jobs);
        return job;
    }

    @Override
    public synchronized Optional<ExportJob> findByUid(String uid) {
        return loadJobs().stream().filter(job -> uid != null && uid.equals(job.getUid())).findFirst();
    }

    @Override
    public synchronized long countInProgress() {
        return loadJobs().stream().filter(job -> ExportStatus.IN_PROGRESS.equals(job.getStatus())).count();
    }

    @Override
    public synchronized List<ExportJob> claimNext(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<ExportJob> jobs = loadJobs();
        List<ExportJob> claimed = new ArrayList<>();
        for (ExportJob job : jobs) {
            if (job.getStatus() == ExportStatus.NEW) {
                job.setStatus(ExportStatus.IN_PROGRESS);
                job.setStartedAt(Instant.now());
                if (job.getResponse() != null) {
                    job.getResponse().setStatus(ExportStatus.IN_PROGRESS);
                    job.getResponse().setStartedAt(job.getStartedAt());
                }
                claimed.add(job);
                if (claimed.size() >= limit) {
                    break;
                }
            }
        }
        if (!claimed.isEmpty()) {
            saveJobs(jobs);
        }
        return claimed;
    }

    @Override
    public synchronized void updateProgress(String uid, long rowCount) {
        update(uid, job -> {
            ExportResponse response = ensureResponse(job);
            response.setStatus(ExportStatus.IN_PROGRESS);
            response.setRowCount(rowCount);
            response.setStartedAt(job.getStartedAt());
        });
    }

    @Override
    public synchronized void markCompleted(String uid, ExportResponse response) {
        update(uid, job -> {
            job.setStatus(ExportStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            ExportResponse normalized = response == null ? new ExportResponse() : response;
            normalized.setUid(uid);
            normalized.setStatus(ExportStatus.COMPLETED);
            normalized.setCompletedAt(job.getCompletedAt());
            normalized.setCreatedAt(job.getCreatedAt());
            normalized.setStartedAt(job.getStartedAt());
            job.setResponse(normalized);
        });
    }

    @Override
    public synchronized void markError(String uid, String errorMessage) {
        update(uid, job -> {
            job.setStatus(ExportStatus.ERROR);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(errorMessage);
            ExportResponse response = ensureResponse(job);
            response.setUid(uid);
            response.setStatus(ExportStatus.ERROR);
            response.setErrorMessage(errorMessage);
            response.setCompletedAt(job.getCompletedAt());
        });
    }

    private void update(String uid, Consumer<ExportJob> updater) {
        List<ExportJob> jobs = loadJobs();
        boolean updated = false;
        for (ExportJob job : jobs) {
            if (uid != null && uid.equals(job.getUid())) {
                updater.accept(job);
                updated = true;
                break;
            }
        }
        if (!updated) {
            throw new CoredeuxExportException("Unknown export uid: " + uid);
        }
        saveJobs(jobs);
    }

    private ExportResponse pendingResponse(ExportRequest request, ExportStatus status, String uid) {
        ExportOptions options = request == null ? null : request.getOptions();
        ExportResponse response = ExportResponse.builder()
                .uid(uid)
                .status(status)
                .format(options == null ? null : options.getFormat())
                .fileName(options == null ? null : options.getFileName())
                .build();
        if (request != null && request.getFieldList() != null) {
            response.setFields(request.getFieldList().stream().map(field -> field.getPath()).toList());
        }
        return response;
    }

    private ExportResponse ensureResponse(ExportJob job) {
        if (job.getResponse() == null) {
            job.setResponse(pendingResponse(job.getRequest(), job.getStatus(), job.getUid()));
        }
        return job.getResponse();
    }

    private List<ExportJob> loadJobs() {
        try {
            if (!Files.exists(queueFile)) {
                return new ArrayList<>();
            }
            String content = Files.readString(queueFile, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return new ArrayList<>();
            }
            List<ExportJob> jobs = objectMapper.readValue(content, new TypeReference<List<ExportJob>>() {
            });
            jobs.sort(Comparator.comparing(ExportJob::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
            return new ArrayList<>(jobs);
        } catch (IOException exception) {
            throw new CoredeuxExportException("Unable to read export queue file", exception);
        }
    }

    private void saveJobs(List<ExportJob> jobs) {
        try {
            Files.createDirectories(queueFile.getParent());
            Path tempFile = Files.createTempFile(queueFile.getParent(), "export-queue-", ".json");
            try {
                Files.writeString(tempFile, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jobs),
                        StandardCharsets.UTF_8);
                try {
                    Files.move(tempFile, queueFile, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException atomicMoveException) {
                    Files.move(tempFile, queueFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException exception) {
            throw new CoredeuxExportException("Unable to persist export queue file", exception);
        }
    }
}
