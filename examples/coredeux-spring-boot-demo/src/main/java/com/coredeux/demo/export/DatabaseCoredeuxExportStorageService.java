package com.coredeux.demo.export;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportStorageArtifact;
import com.coredeux.export.model.ExportStorageRequest;
import com.coredeux.export.storage.CoredeuxExportStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("databaseCoredeuxExportStorageService")
public class DatabaseCoredeuxExportStorageService implements CoredeuxExportStorageService {

    private final ExportStorageRecordRepository repository;
    private final ObjectMapper objectMapper;

    public DatabaseCoredeuxExportStorageService(ExportStorageRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ExportStorageArtifact store(ExportStorageRequest request) {
        if (request == null || request.getSourceFile() == null) {
            throw new CoredeuxExportException("Export storage request must include a source file");
        }
        try {
            byte[] content = Files.readAllBytes(request.getSourceFile());
            String uid = normalizeUid(request.getUid(), request.getFileName());
            Map<String, Object> metadata = metadata(request, uid, content.length);
            ExportStorageRecord record = ExportStorageRecord.builder()
                    .uid(uid)
                    .fileName(request.getFileName())
                    .storageType("database")
                    .contentType(request.getContentType())
                    .size((long) content.length)
                    .content(content)
                    .metadataJson(objectMapper.writeValueAsString(metadata))
                    .url("/api/export/" + uid + "/download")
                    .canonicalUrl("/api/export/" + uid + "/download")
                    .build();
            repository.save(record);
            return ExportStorageArtifact.builder()
                    .storageType(record.getStorageType())
                    .fileName(record.getFileName())
                    .absolutePath(null)
                    .relativePath("export-storage:" + uid)
                    .url(record.getUrl())
                    .canonicalUrl(record.getCanonicalUrl())
                    .size(record.getSize())
                    .contentType(record.getContentType())
                    .metadata(metadata)
                    .build();
        } catch (IOException exception) {
            throw new CoredeuxExportException("Unable to store export file in database", exception);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ExportStorageRecord> findByUid(String uid) {
        return repository.findByUid(uid);
    }

    private String normalizeUid(String uid, String fallback) {
        if (uid != null && !uid.isBlank()) {
            return uid.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        throw new CoredeuxExportException("Export storage request uid must not be blank");
    }

    private Map<String, Object> metadata(ExportStorageRequest request, String uid, long size) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("uid", uid);
        metadata.put("fileName", request.getFileName());
        metadata.put("contentType", request.getContentType());
        metadata.put("size", size);
        metadata.put("format", request.getFormat() == null ? null : request.getFormat().name());
        metadata.put("storage", "database");
        return metadata;
    }
}
