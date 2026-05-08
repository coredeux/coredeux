package com.coredeux.export.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uid;
    private ExportStatus status;
    private ExportFormat format;
    private String fileName;
    private String contentType;
    private ExportStorageArtifact storage;
    private long rowCount;
    private String errorMessage;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

    @Builder.Default
    private List<String> fields = new ArrayList<>();

    @Builder.Default
    private List<ExportLogEntry> logs = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
