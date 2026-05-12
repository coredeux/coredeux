package com.coredeux.impex.parser;

import com.coredeux.impex.model.ImportRequest;

public interface CoredeuxImportParser<T> {

    /**
     * Converts an external import representation into the canonical
     * {@link ImportRequest} model consumed by {@code coredeux-import}. Optional
     * parser arguments are source-specific; for example, the Excel parser uses
     * the first argument as the workbook sheet name.
     */
    ImportRequest parse(T source, String... args);
}
