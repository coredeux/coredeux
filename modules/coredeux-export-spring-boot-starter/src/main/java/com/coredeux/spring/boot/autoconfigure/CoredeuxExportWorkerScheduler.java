package com.coredeux.spring.boot.autoconfigure;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.annotation.Scheduled;

import com.coredeux.export.worker.DefaultCoredeuxExportWorker;

public class CoredeuxExportWorkerScheduler implements DisposableBean {

    private final DefaultCoredeuxExportWorker worker;

    public CoredeuxExportWorkerScheduler(DefaultCoredeuxExportWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "#{@coredeuxExportProperties.workerDelayMs}")
    public void poll() {
        worker.processPendingExports();
    }

    @Override
    public void destroy() throws Exception {
        worker.shutdown();
    }
}
