package com.coredeux.impex.handler;

import java.util.Map;

import com.coredeux.impex.exception.CoredeuxImportException;

public class ImportValueHandlerResolver {

    public static final String DEFAULT_HANDLER = "coredeuxDefaultImportValueHandler";

    private final Map<String, CoredeuxImportValueHandler> handlers;

    /**
     * Captures import handlers by component name.
     *
     * <p>
     * The import service uses this resolver so JSON payloads and parsed files can
     * reference handlers by name without coupling to a dependency injection
     * container.
     */
    public ImportValueHandlerResolver(Map<String, CoredeuxImportValueHandler> handlers) {
        this.handlers = handlers == null ? Map.of() : handlers;
    }

    /**
     * Resolves the requested handler name, falling back to the default converter
     * when the column does not specify a handler.
     */
    public CoredeuxImportValueHandler resolve(String handlerName) {
        String resolvedName = handlerName == null || handlerName.isBlank() ? DEFAULT_HANDLER : handlerName.trim();
        CoredeuxImportValueHandler handler = handlers.get(resolvedName);
        if (handler == null) {
            throw new CoredeuxImportException("Import value handler not found: " + resolvedName);
        }
        return handler;
    }
}
