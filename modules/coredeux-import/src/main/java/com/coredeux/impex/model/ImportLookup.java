package com.coredeux.impex.model;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportLookup {

    /**
     * Target search field used in the generated SearchParams.
     */
    private String field;

    /**
     * Search comparator used in the generated SearchParams.
     */
    @Builder.Default
    private String comparator = "EQUALS";

    /**
     * Optional source import column. When omitted, the service resolves a column
     * whose name matches field.
     */
    private String column;

    /**
     * When the derived lookup value is null, use ISNULL instead of the configured
     * comparator.
     */
    private boolean nullSearch;

    /**
     * Extension bag for parser/module-specific lookup metadata.
     */
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
