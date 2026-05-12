package com.coredeux.export.log.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.model.ExportLogEntry;
import com.coredeux.export.support.ExportJsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FileCoredeuxExportLogService implements CoredeuxExportLogService {

    public static final String DEFAULT_BASE_DIRECTORY = Path.of(System.getProperty("java.io.tmpdir"), "coredeux-export",
            "logs").toString();

    private final ObjectMapper objectMapper = ExportJsonSupport.objectMapper();
    private final Path baseDirectory;

    public FileCoredeuxExportLogService() {
        this(DEFAULT_BASE_DIRECTORY);
    }

    public FileCoredeuxExportLogService(String baseDirectory) {
        String resolvedBaseDirectory = baseDirectory == null || baseDirectory.isBlank() ? DEFAULT_BASE_DIRECTORY
                : baseDirectory;
        this.baseDirectory = Path.of(resolvedBaseDirectory);
    }

    @Override
    public void info(String uid, String message, Map<String, Object> metadata) {
        append(uid, "INFO", message, null, metadata);
    }

    @Override
    public void warn(String uid, String message, Map<String, Object> metadata) {
        append(uid, "WARN", message, null, metadata);
    }

    @Override
    public void error(String uid, String message, Throwable error, Map<String, Object> metadata) {
        append(uid, "ERROR", message, error, metadata);
    }

    @Override
    public List<ExportLogEntry> findByUid(String uid) {
        Path logFile = logFile(uid);
        if (!Files.exists(logFile)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            List<ExportLogEntry> entries = new ArrayList<>();
            for (String line : lines) {
                if (line != null && !line.isBlank()) {
                    entries.add(objectMapper.readValue(line, ExportLogEntry.class));
                }
            }
            return entries;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read export logs", exception);
        }
    }

    private void append(String uid, String level, String message, Throwable error, Map<String, Object> metadata) {
        ExportLogEntry entry = ExportLogEntry.builder()
                .uid(uid)
                .timestamp(Instant.now())
                .level(level)
                .message(message)
                .exceptionType(error == null ? null : error.getClass().getName())
                .stackTrace(error == null ? null : stackTrace(error))
                .metadata(metadata == null ? Map.of() : metadata)
                .build();
        Path logFile = logFile(uid);
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, objectMapper.writeValueAsString(entry) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write export log", exception);
        }
    }

    private Path logFile(String uid) {
        return baseDirectory.resolve(uid + ".jsonl");
    }

    private String stackTrace(Throwable error) {
        StringBuilder builder = new StringBuilder();
        builder.append(error).append(System.lineSeparator());
        for (StackTraceElement element : error.getStackTrace()) {
            builder.append("at ").append(element).append(System.lineSeparator());
        }
        return builder.toString();
    }
}
