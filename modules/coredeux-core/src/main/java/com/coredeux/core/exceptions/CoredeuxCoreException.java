package com.coredeux.core.exceptions;

/**
 * Base runtime exception for errors raised by the Coredeux core module.
 */
public class CoredeuxCoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public CoredeuxCoreException() {
    }

    public CoredeuxCoreException(String message) {
        super(message);
    }

    public CoredeuxCoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoredeuxCoreException(Throwable cause) {
        super(cause);
    }
}
