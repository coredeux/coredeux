package com.coredeux.export.storage.impl;

import java.util.Map;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.storage.CoredeuxExportStorageService;
import com.coredeux.export.storage.CoredeuxExportStorageServiceResolver;

public class DefaultCoredeuxExportStorageServiceResolver implements CoredeuxExportStorageServiceResolver {

    public static final String DEFAULT_STORAGE_SERVICE = "defaultCoredeuxExportStorageService";

    private final Map<String, CoredeuxExportStorageService> services;
    private final String defaultStorageService;

    public DefaultCoredeuxExportStorageServiceResolver(Map<String, CoredeuxExportStorageService> services) {
        this(services, DEFAULT_STORAGE_SERVICE);
    }

    public DefaultCoredeuxExportStorageServiceResolver(Map<String, CoredeuxExportStorageService> services,
            String defaultStorageService) {
        this.services = services;
        this.defaultStorageService = defaultStorageService == null || defaultStorageService.isBlank()
                ? DEFAULT_STORAGE_SERVICE
                : defaultStorageService;
    }

    @Override
    public CoredeuxExportStorageService resolve(String storageServiceName) {
        String candidate = storageServiceName == null || storageServiceName.isBlank()
                ? defaultStorageService
                : storageServiceName.trim();
        CoredeuxExportStorageService service = services.get(candidate);
        if (service == null && services.size() == 1) {
            return services.values().iterator().next();
        }
        if (service == null) {
            throw new CoredeuxExportException("Unable to resolve export storage service: " + candidate);
        }
        return service;
    }
}
