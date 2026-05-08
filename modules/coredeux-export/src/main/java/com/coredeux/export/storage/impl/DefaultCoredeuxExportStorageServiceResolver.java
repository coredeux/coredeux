package com.coredeux.export.storage.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.storage.CoredeuxExportStorageService;
import com.coredeux.export.storage.CoredeuxExportStorageServiceResolver;

@Component
public class DefaultCoredeuxExportStorageServiceResolver implements CoredeuxExportStorageServiceResolver {

    private final Map<String, CoredeuxExportStorageService> services;
    private final String defaultStorageService;

    public DefaultCoredeuxExportStorageServiceResolver(Map<String, CoredeuxExportStorageService> services,
            @Value("${coredeux.export.storage.default-service:defaultCoredeuxExportStorageService}") String defaultStorageService) {
        this.services = services;
        this.defaultStorageService = defaultStorageService;
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
