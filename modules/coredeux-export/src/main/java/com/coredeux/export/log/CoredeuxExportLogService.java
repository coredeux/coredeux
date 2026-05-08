package com.coredeux.export.log;

import java.util.List;
import java.util.Map;

import com.coredeux.export.model.ExportLogEntry;

public interface CoredeuxExportLogService {

    void info(String uid, String message, Map<String, Object> metadata);

    void warn(String uid, String message, Map<String, Object> metadata);

    void error(String uid, String message, Throwable error, Map<String, Object> metadata);

    List<ExportLogEntry> findByUid(String uid);
}
