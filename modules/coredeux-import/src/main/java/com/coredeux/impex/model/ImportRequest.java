package com.coredeux.impex.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRequest {

    /**
     * Named macro values available to statements, columns, handlers, and rows.
     */
    @Builder.Default
    private Map<String, ImportMacro> macros = new LinkedHashMap<>();

    /**
     * Request-level execution options.
     */
    @Builder.Default
    private ImportOptions options = ImportOptions.builder().build();

    /**
     * Ordered import statement blocks. Statements are executed in this order on each
     * pass.
     */
    @Builder.Default
    private List<ImportStatement> statements = new ArrayList<>();
}
