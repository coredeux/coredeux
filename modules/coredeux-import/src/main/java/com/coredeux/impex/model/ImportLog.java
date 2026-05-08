package com.coredeux.impex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportLog {

    /**
     * Severity of the import message.
     */
    private ImportSeverity severity;

    /**
     * Human-readable message suitable for API responses and diagnostics.
     */
    private String message;

    /**
     * One-based statement index in the request, when the log can be tied to a
     * statement.
     */
    private Integer statementIndex;

    /**
     * One-based row index inside the statement, when the log can be tied to a row.
     */
    private Integer rowIndex;

    /**
     * Entity involved in the message.
     */
    private String entity;

    /**
     * Column involved in the message, when applicable.
     */
    private String column;

    /**
     * Exception class name captured for diagnostics without serializing the full
     * exception.
     */
    private String exceptionType;
}
