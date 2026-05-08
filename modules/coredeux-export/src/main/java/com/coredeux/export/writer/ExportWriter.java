package com.coredeux.export.writer;

import java.nio.file.Path;

import com.coredeux.export.model.ExportOptions;

public interface ExportWriter {

    ExportWriteSession open(Path targetFile, ExportOptions options);

    String contentType();

    String extension();
}
