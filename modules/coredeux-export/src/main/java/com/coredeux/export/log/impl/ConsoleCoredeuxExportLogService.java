package com.coredeux.export.log.impl;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.model.ExportLogEntry;

@Service("consoleCoredeuxExportLogService")
public class ConsoleCoredeuxExportLogService implements CoredeuxExportLogService {

    private static final Logger LOG = Logger.getLogger(ConsoleCoredeuxExportLogService.class.getName());

    @Override
    public void info(String uid, String message, Map<String, Object> metadata) {
        log("INFO", uid, message, metadata, null);
    }

    @Override
    public void warn(String uid, String message, Map<String, Object> metadata) {
        log("WARN", uid, message, metadata, null);
    }

    @Override
    public void error(String uid, String message, Throwable error, Map<String, Object> metadata) {
        log("ERROR", uid, message, metadata, error);
    }

    @Override
    public List<ExportLogEntry> findByUid(String uid) {
        return List.of();
    }

    private void log(String level, String uid, String message, Map<String, Object> metadata, Throwable error) {
        String payload = "uid=" + uid + ", message=" + message + ", metadata=" + (metadata == null ? Map.of() : metadata);
        if (error == null) {
            LOG.log(toLevel(level), payload);
            return;
        }
        LOG.log(toLevel(level), payload, error);
    }

    private Level toLevel(String level) {
        return switch (level) {
            case "WARN" -> Level.WARNING;
            case "ERROR" -> Level.SEVERE;
            default -> Level.INFO;
        };
    }
}
