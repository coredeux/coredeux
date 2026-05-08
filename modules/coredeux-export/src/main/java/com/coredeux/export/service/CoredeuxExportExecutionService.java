package com.coredeux.export.service;

import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;

public interface CoredeuxExportExecutionService {

    ExportResponse execute(String uid, ExportRequest request);
}
