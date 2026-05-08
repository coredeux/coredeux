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
public class ImportRow {

    /**
     * Optional row-level reference key that can be used by later rows, such as
     * &customer1.
     */
    private String key;

    /**
     * Column values keyed by ImportColumn.name.
     */
    @Builder.Default
    private Map<String, Object> values = new LinkedHashMap<>();

    /**
     * Extension bag for parser/module-specific row metadata.
     */
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
