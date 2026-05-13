package com.coredeux.demo.hooks;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.demo.export.ExportStorageRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cleans up filesystem-backed export artifacts before the storage row is deleted.
 * <p>
 * Database-backed export rows do not need any external cleanup, so this hook only
 * attempts deletion when the record clearly points to a file on disk.
 */
public class ExportStorageCleanupHook implements CoredeuxEntityHook<ExportStorageRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(ExportStorageCleanupHook.class);

    private final ObjectMapper objectMapper;

    public ExportStorageCleanupHook(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void beforeDelete(ExportStorageRecord entity, CoredeuxEntityDefinition definition, OperationContext context) {
        if (entity == null) {
            return;
        }
        cleanupFilesystemArtifact(entity).ifPresent(path -> {
            try {
                if (Files.deleteIfExists(path)) {
                    LOG.info("Deleted export artifact at {}", path.toAbsolutePath());
                } else {
                    LOG.debug("Export artifact did not exist at {}", path.toAbsolutePath());
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to delete export artifact at " + path.toAbsolutePath(),
                        exception);
            }
        });
    }

    private Optional<Path> cleanupFilesystemArtifact(ExportStorageRecord entity) {
        if (!isFilesystemStorage(entity)) {
            return Optional.empty();
        }
        Map<String, Object> metadata = parseMetadata(entity.getMetadataJson());
        Path path = resolvePath(metadata, entity);
        if (path == null) {
            return Optional.empty();
        }
        return Optional.of(path);
    }

    private boolean isFilesystemStorage(ExportStorageRecord entity) {
        String storageType = entity.getStorageType();
        return storageType != null && "filesystem".equalsIgnoreCase(storageType.trim());
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (IOException exception) {
            LOG.warn("Unable to parse export storage metadata JSON; falling back to direct record fields", exception);
            return Map.of();
        }
    }

    private Path resolvePath(Map<String, Object> metadata, ExportStorageRecord entity) {
        String candidate = firstNonBlank(
                stringValue(metadata.get("destination")),
                stringValue(metadata.get("absolutePath")),
                stringValue(metadata.get("path")),
                stringValue(metadata.get("filePath")),
                entity.getCanonicalUrl(),
                entity.getUrl());
        if (candidate == null) {
            return null;
        }
        if (candidate.startsWith("file:")) {
            try {
                return Paths.get(new URI(candidate));
            } catch (URISyntaxException exception) {
                throw new IllegalStateException("Invalid file URI for export artifact: " + candidate, exception);
            }
        }
        Path path = Path.of(candidate);
        return Files.exists(path) ? path : null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
