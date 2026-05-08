package com.coredeux.export.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJob implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uid;
    private ExportStatus status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private ExportRequest request;
    private ExportResponse response;

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
