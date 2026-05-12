package com.coredeux.export.log.impl;

import java.util.Map;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.log.CoredeuxExportLogServiceResolver;

public class DefaultCoredeuxExportLogServiceResolver implements CoredeuxExportLogServiceResolver {

    public static final String DEFAULT_LOG_SERVICE = "defaultCoredeuxExportLogService";

    private final Map<String, CoredeuxExportLogService> services;
    private final String defaultLogService;

    public DefaultCoredeuxExportLogServiceResolver(Map<String, CoredeuxExportLogService> services) {
        this(services, DEFAULT_LOG_SERVICE);
    }

    public DefaultCoredeuxExportLogServiceResolver(Map<String, CoredeuxExportLogService> services,
            String defaultLogService) {
        this.services = services;
        this.defaultLogService = defaultLogService == null || defaultLogService.isBlank()
                ? DEFAULT_LOG_SERVICE
                : defaultLogService;
    }

    @Override
    public CoredeuxExportLogService resolve(String logServiceName) {
        String candidate = logServiceName == null || logServiceName.isBlank()
                ? defaultLogService
                : logServiceName.trim();
        CoredeuxExportLogService service = services.get(candidate);
        if (service == null && services.size() == 1) {
            return services.values().iterator().next();
        }
        if (service == null) {
            throw new CoredeuxExportException("Unable to resolve export log service: " + candidate);
        }
        return service;
    }
}
