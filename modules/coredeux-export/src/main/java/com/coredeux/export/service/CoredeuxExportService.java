package com.coredeux.export.service;

import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;

public interface CoredeuxExportService {

    ExportResponse queueExport(ExportRequest request);

    ExportResponse getExport(String uid);
}
