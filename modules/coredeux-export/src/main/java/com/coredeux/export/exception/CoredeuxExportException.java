package com.coredeux.export.exception;

import com.coredeux.core.exceptions.CoredeuxCoreException;

public class CoredeuxExportException extends CoredeuxCoreException {

    private static final long serialVersionUID = 1L;

    public CoredeuxExportException(String message) {
        super(message);
    }

    public CoredeuxExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
