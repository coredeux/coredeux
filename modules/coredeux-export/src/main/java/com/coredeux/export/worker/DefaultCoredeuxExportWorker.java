package com.coredeux.export.worker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.log.CoredeuxExportLogServiceResolver;
import com.coredeux.export.model.ExportJob;
import com.coredeux.export.queue.CoredeuxExportQueueService;
import com.coredeux.export.service.CoredeuxExportExecutionService;

@Component
public class DefaultCoredeuxExportWorker implements DisposableBean {

    private final CoredeuxExportQueueService queueService;
    private final CoredeuxExportExecutionService executionService;
    private final CoredeuxExportLogServiceResolver logServiceResolver;
    private final ExecutorService executorService;
    private final int maxParallel;
    private final boolean enabled;
    private final AtomicBoolean polling = new AtomicBoolean();

    public DefaultCoredeuxExportWorker(CoredeuxExportQueueService queueService,
            CoredeuxExportExecutionService executionService, CoredeuxExportLogServiceResolver logServiceResolver,
            @Value("${coredeux.export.worker.max-parallel:2}") int maxParallel,
            @Value("${coredeux.export.worker.enabled:true}") boolean enabled) {
        this.queueService = queueService;
        this.executionService = executionService;
        this.logServiceResolver = logServiceResolver;
        this.maxParallel = Math.max(1, maxParallel);
        this.enabled = enabled;
        this.executorService = Executors.newFixedThreadPool(this.maxParallel);
    }

    @Scheduled(fixedDelayString = "${coredeux.export.worker.delay-ms:5000}")
    public void poll() {
        processPendingExports();
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
                executorService.submit(() -> executionService.execute(job.getUid(), job.getRequest()));
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

    @Override
    public void destroy() throws Exception {
        shutdown();
    }
}
