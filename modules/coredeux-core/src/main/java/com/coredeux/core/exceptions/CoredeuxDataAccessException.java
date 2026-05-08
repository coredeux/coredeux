package com.coredeux.core.exceptions;

/**
 * Exception raised for persistence and repository access failures.
 */
public class CoredeuxDataAccessException extends CoredeuxCoreException {

    private static final long serialVersionUID = 1L;

	public CoredeuxDataAccessException() {
    }

    public CoredeuxDataAccessException(String message) {
        super(message);
    }

    public CoredeuxDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoredeuxDataAccessException(Throwable cause) {
        super(cause);
    }
}
