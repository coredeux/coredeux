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
public class ImportMacro {

    /**
     * Macro replacement value. References to this macro resolve to this value during
     * import processing.
     */
    private String value;

    /**
     * Extension bag for parser/module-specific macro metadata.
     */
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
