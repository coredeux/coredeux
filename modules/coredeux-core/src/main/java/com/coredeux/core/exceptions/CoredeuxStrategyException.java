package com.coredeux.core.exceptions;

/**
 * Exception raised for strategy execution and orchestration failures.
 */
public class CoredeuxStrategyException extends CoredeuxCoreException {

    private static final long serialVersionUID = 1L;

	public CoredeuxStrategyException() {
    }

    public CoredeuxStrategyException(String message) {
        super(message);
    }

    public CoredeuxStrategyException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoredeuxStrategyException(Throwable cause) {
        super(cause);
    }
}
