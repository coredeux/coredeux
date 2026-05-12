package com.coredeux.impex.parser.exception;

public class CoredeuxImportParserException extends RuntimeException {

    /**
     * Creates a parser exception for syntax or shape errors that can be explained
     * directly to the import-file author.
     */
    public CoredeuxImportParserException(String message) {
        super(message);
    }

    /**
     * Creates a parser exception that preserves the lower-level cause while still
     * exposing a parser-friendly message.
     */
    public CoredeuxImportParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
