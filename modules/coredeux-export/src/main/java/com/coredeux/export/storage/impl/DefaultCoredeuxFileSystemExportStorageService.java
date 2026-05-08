package com.coredeux.export.storage.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportStorageArtifact;
import com.coredeux.export.model.ExportStorageRequest;
import com.coredeux.export.storage.CoredeuxExportStorageService;

@Service("defaultCoredeuxExportStorageService")
public class DefaultCoredeuxFileSystemExportStorageService implements CoredeuxExportStorageService {

    private static final String DEFAULT_BASE_DIRECTORY = "${java.io.tmpdir}/coredeux-export";

    private final Path baseDirectory;

    public DefaultCoredeuxFileSystemExportStorageService(
            @Value("${coredeux.export.storage.filesystem.base-directory:" + DEFAULT_BASE_DIRECTORY + "}")
            String baseDirectory) {
        this.baseDirectory = Path.of(baseDirectory);
    }

    @Override
    public ExportStorageArtifact store(ExportStorageRequest request) {
        if (request == null || request.getSourceFile() == null) {
            throw new CoredeuxExportException("Export storage request must include a source file");
        }
        try {
            Files.createDirectories(baseDirectory);
            Path destination = baseDirectory.resolve(request.getFileName());
            Files.copy(request.getSourceFile(), destination, StandardCopyOption.REPLACE_EXISTING);
            long size = Files.size(destination);
            return ExportStorageArtifact.builder()
                    .storageType("filesystem")
                    .fileName(request.getFileName())
                    .absolutePath(destination.toAbsolutePath().toString())
                    .relativePath(baseDirectory.relativize(destination).toString())
                    .url(destination.toUri().toString())
                    .canonicalUrl(destination.toUri().toString())
                    .size(size)
                    .contentType(request.getContentType())
                    .metadata(metadata(destination, size, request))
                    .build();
        } catch (IOException exception) {
            throw new CoredeuxExportException("Unable to store export file", exception);
        }
    }

    private Map<String, Object> metadata(Path destination, long size, ExportStorageRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("baseDirectory", baseDirectory.toAbsolutePath().toString());
        metadata.put("destination", destination.toAbsolutePath().toString());
        metadata.put("size", size);
        metadata.put("format", request.getFormat() == null ? null : request.getFormat().name());
        metadata.put("uid", request.getUid());
        return metadata;
    }
}
