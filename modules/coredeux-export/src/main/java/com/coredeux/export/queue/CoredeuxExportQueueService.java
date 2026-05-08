package com.coredeux.export.queue;

import java.util.List;
import java.util.Optional;

import com.coredeux.export.model.ExportJob;
import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;

public interface CoredeuxExportQueueService {

    ExportJob enqueue(ExportRequest request);

    Optional<ExportJob> findByUid(String uid);

    long countInProgress();

    List<ExportJob> claimNext(int limit);

    void updateProgress(String uid, long rowCount);

    void markCompleted(String uid, ExportResponse response);

    void markError(String uid, String errorMessage);
}
