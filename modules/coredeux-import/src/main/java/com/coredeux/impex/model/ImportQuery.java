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
public class ImportQuery {

    /**
     * Store-specific query text passed through to CoredeuxService.query.
     */
    private String text;

    /**
     * Query parameters derived from row columns.
     */
    @Builder.Default
    private Map<String, ImportQueryParam> params = new LinkedHashMap<>();

    /**
     * Extension bag for parser/module-specific query metadata.
     */
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
