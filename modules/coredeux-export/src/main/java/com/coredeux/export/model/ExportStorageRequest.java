package com.coredeux.export.model;

import java.io.Serializable;
import java.nio.file.Path;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportStorageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Path sourceFile;
    private String fileName;
    private String contentType;
    private ExportFormat format;
    private String uid;
}
