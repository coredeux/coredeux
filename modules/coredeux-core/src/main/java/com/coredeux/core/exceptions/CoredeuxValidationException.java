package com.coredeux.core.exceptions;

import java.util.List;

import com.coredeux.core.validation.ValidationError;

/**
 * Exception raised when caller input or entity state fails framework validation.
 */
public class CoredeuxValidationException extends CoredeuxCoreException {

    private static final long serialVersionUID = 1L;

    private final List<ValidationError> validationErrors;

    public CoredeuxValidationException() {
        this.validationErrors = List.of();
    }

    public CoredeuxValidationException(String message) {
        super(message);
        this.validationErrors = List.of();
    }

    public CoredeuxValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = List.of();
    }

    public CoredeuxValidationException(Throwable cause) {
        super(cause);
        this.validationErrors = List.of();
    }

    public CoredeuxValidationException(String message, List<ValidationError> validationErrors) {
        super(message);
        this.validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
}
