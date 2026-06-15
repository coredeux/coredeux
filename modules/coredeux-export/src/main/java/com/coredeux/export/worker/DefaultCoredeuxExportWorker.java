package com.coredeux.export.worker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.log.CoredeuxExportLogServiceResolver;
import com.coredeux.export.model.ExportJob;
import com.coredeux.export.queue.CoredeuxExportQueueService;
import com.coredeux.export.service.CoredeuxExportExecutionService;

public class DefaultCoredeuxExportWorker {

    private final CoredeuxExportQueueService queueService;
    private final CoredeuxExportExecutionService executionService;
    private final CoredeuxExportLogServiceResolver logServiceResolver;
    private final ExecutorService executorService;
    private final int maxParallel;
    private final boolean enabled;
    private final AtomicBoolean polling = new AtomicBoolean();

    public DefaultCoredeuxExportWorker(CoredeuxExportQueueService queueService,
            CoredeuxExportExecutionService executionService, CoredeuxExportLogServiceResolver logServiceResolver,
            int maxParallel, boolean enabled) {
        this.queueService = queueService;
        this.executionService = executionService;
        this.logServiceResolver = logServiceResolver;
        this.maxParallel = Math.max(1, maxParallel);
        this.enabled = enabled;
        this.executorService = Executors.newFixedThreadPool(this.maxParallel);
    }

    public void processPendingExports() {
        if (!enabled || !polling.compareAndSet(false, true)) {
            return;
        }
        try {
            int availableSlots = (int) Math.max(0, maxParallel - queueService.countInProgress());
            List<ExportJob> jobs = queueService.claimNext(availableSlots);
            CoredeuxExportLogService logService = logServiceResolver.resolve(null);
            for (ExportJob job : jobs) {
                executorService.submit(() -> executeSafely(job, logService));
                logService.info(job.getUid(), "Export job dispatched to worker", null);
            }
        } finally {
            polling.set(false);
        }
    }

    public void shutdown() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    private void executeSafely(ExportJob job, CoredeuxExportLogService logService) {
        try {
            executionService.execute(job.getUid(), job.getRequest());
        } catch (RuntimeException exception) {
            queueService.markError(job.getUid(), exception.getMessage());
            logService.error(job.getUid(), "Export worker execution failed", exception, null);
        }
    }
}
