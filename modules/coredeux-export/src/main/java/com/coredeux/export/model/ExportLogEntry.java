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
public class ExportLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uid;
    private Instant timestamp;
    private String level;
    private String message;
    private String exceptionType;
    private String stackTrace;

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
