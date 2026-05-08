package com.coredeux.impex.exception;

import com.coredeux.core.exceptions.CoredeuxCoreException;

public class CoredeuxImportException extends CoredeuxCoreException {

    private static final long serialVersionUID = 1L;

    private final String column;

    /**
     * Creates an import exception for errors that are not tied to a specific
     * column.
     */
    public CoredeuxImportException(String message) {
        super(message);
        this.column = null;
    }

    /**
     * Creates an import exception with a root cause when no column context is
     * available.
     */
    public CoredeuxImportException(String message, Throwable cause) {
        super(message, cause);
        this.column = null;
    }

    /**
     * Creates an import exception that points error reporting at a specific import
     * column.
     */
    public CoredeuxImportException(String message, String column) {
        super(message);
        this.column = column;
    }

    /**
     * Creates an import exception with both a root cause and a column name for
     * precise row-level logging.
     */
    public CoredeuxImportException(String message, String column, Throwable cause) {
        super(message, cause);
        this.column = column;
    }

    /**
     * Returns the import column related to this failure, or {@code null} for
     * statement/request-level failures.
     */
    public String getColumn() {
        return column;
    }
}
