package com.coredeux.drl.exceptions;

import com.coredeux.core.exceptions.CoredeuxCoreException;

/**
 * Base runtime exception for errors raised by the Coredeux DRL module.
 */
public class CoredeuxDRLException extends CoredeuxCoreException {

    private static final long serialVersionUID = 1L;

    public CoredeuxDRLException() {
    }

    public CoredeuxDRLException(String message) {
        super(message);
    }

    public CoredeuxDRLException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoredeuxDRLException(Throwable cause) {
        super(cause);
    }
}
