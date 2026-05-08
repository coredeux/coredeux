package com.coredeux.impex.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {

    /**
     * Validation and execution messages collected during import processing.
     */
    @Builder.Default
    private List<ImportLog> logs = new ArrayList<>();

    public boolean hasErrors() {
        return logs.stream().anyMatch(log -> ImportSeverity.ERROR.equals(log.getSeverity())
                || ImportSeverity.CRITICAL.equals(log.getSeverity()));
    }
}
