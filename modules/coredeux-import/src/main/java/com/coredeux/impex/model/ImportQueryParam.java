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
public class ImportQueryParam {

    /**
     * Optional source import column. When omitted, the parameter name is used as
     * the source column name.
     */
    private String column;

    /**
     * Extension bag for parser/module-specific query parameter metadata.
     */
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
