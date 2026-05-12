package com.coredeux.export.handler;

import java.util.Map;

import com.coredeux.export.exception.CoredeuxExportException;

public class ExportValueHandlerResolver {

    private final Map<String, CoredeuxExportValueHandler> handlers;

    public static final String DEFAULT_HANDLER = "defaultCoredeuxExportValueHandler";

    public ExportValueHandlerResolver(Map<String, CoredeuxExportValueHandler> handlers) {
        this.handlers = handlers;
    }

    public CoredeuxExportValueHandler resolve(String handlerName) {
        String resolvedName = handlerName == null || handlerName.isBlank() ? DEFAULT_HANDLER
                : handlerName.trim();
        CoredeuxExportValueHandler handler = handlers.get(resolvedName);
        if (handler == null) {
            throw new CoredeuxExportException("Unable to resolve export value handler: " + resolvedName);
        }
        return handler;
    }
}
