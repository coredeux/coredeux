package com.coredeux.export.handler;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.coredeux.export.exception.CoredeuxExportException;

@Component
public class ExportValueHandlerResolver {

    private final Map<String, CoredeuxExportValueHandler> handlers;

    public ExportValueHandlerResolver(Map<String, CoredeuxExportValueHandler> handlers) {
        this.handlers = handlers;
    }

    public CoredeuxExportValueHandler resolve(String handlerName) {
        String resolvedName = handlerName == null || handlerName.isBlank() ? "defaultCoredeuxExportValueHandler"
                : handlerName.trim();
        CoredeuxExportValueHandler handler = handlers.get(resolvedName);
        if (handler == null) {
            throw new CoredeuxExportException("Unable to resolve export value handler: " + resolvedName);
        }
        return handler;
    }
}
