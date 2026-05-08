package com.coredeux.export.log.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.log.CoredeuxExportLogServiceResolver;

@Component
public class DefaultCoredeuxExportLogServiceResolver implements CoredeuxExportLogServiceResolver {

    private final Map<String, CoredeuxExportLogService> services;
    private final String defaultLogService;

    public DefaultCoredeuxExportLogServiceResolver(Map<String, CoredeuxExportLogService> services,
            @Value("${coredeux.export.log.default-service:defaultCoredeuxExportLogService}") String defaultLogService) {
        this.services = services;
        this.defaultLogService = defaultLogService;
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
