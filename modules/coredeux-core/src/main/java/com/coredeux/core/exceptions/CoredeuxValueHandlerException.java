package com.coredeux.core.exceptions;

/**
 * Exception raised for value handler resolution and execution failures.
 */
public class CoredeuxValueHandlerException extends CoredeuxCoreException {

    private static final long serialVersionUID = 1L;

    public CoredeuxValueHandlerException() {
    }

    public CoredeuxValueHandlerException(String message) {
        super(message);
    }

    public CoredeuxValueHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoredeuxValueHandlerException(Throwable cause) {
        super(cause);
    }
}
