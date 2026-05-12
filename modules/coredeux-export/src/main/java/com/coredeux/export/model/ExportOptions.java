package com.coredeux.export.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    private ExportFormat format;

    private Integer limit;

    @Builder.Default
    private Integer batchSize = 100;

    @Builder.Default
    private Boolean includeHeader = true;

    @Builder.Default
    private String textSeparator = "|";

    @Builder.Default
    private String collectionSeparator = ", ";

    private String fileName;

    private String storageService;
}
