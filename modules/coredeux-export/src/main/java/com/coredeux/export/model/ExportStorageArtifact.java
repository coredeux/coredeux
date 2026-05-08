package com.coredeux.export.model;

import java.io.Serializable;
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
public class ExportStorageArtifact implements Serializable {

    private static final long serialVersionUID = 1L;

    private String storageType;
    private String fileName;
    private String absolutePath;
    private String relativePath;
    private String url;
    private String canonicalUrl;
    private Long size;
    private String contentType;

    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
