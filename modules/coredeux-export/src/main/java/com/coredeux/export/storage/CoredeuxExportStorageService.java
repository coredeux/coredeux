package com.coredeux.export.storage;

import com.coredeux.export.model.ExportStorageArtifact;
import com.coredeux.export.model.ExportStorageRequest;

public interface CoredeuxExportStorageService {

    ExportStorageArtifact store(ExportStorageRequest request);
}
