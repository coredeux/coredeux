package com.coredeux.impex.service;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;

public interface CoredeuxImportService {

    /**
     * Validates an import request without mutating target data.
     *
     * <p>
     * This is intended for pre-flight checks from controllers, tooling, or file
     * parsers before the caller decides to execute the import.
     */
    ImportResponse validateData(ImportRequest request);

    /**
     * Validates and executes an import request.
     *
     * <p>
     * Implementations are expected to return structured import logs instead of
     * leaking row-level failures directly to the caller.
     */
    ImportResponse importData(ImportRequest request);
}
