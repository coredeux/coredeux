package com.coredeux.impex.handler;

public interface CoredeuxImportValueHandler {

    /**
     * Converts or resolves one column value for the current import row.
     *
     * <p>
     * Handlers may perform simple type conversion, reference lookup, normalization,
     * or application-specific parsing, but should return a value assignable to the
     * target field described by the context.
     */
    Object handle(ImportValueContext context);
}
