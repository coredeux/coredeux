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
public class ImportStatement {

    /**
     * Operation to perform for this statement block.
     */
    private ImportOperation operation;

    /**
     * Fully qualified Java class name of the entity imported by this statement.
     */
    private String entity;

    /**
     * Ordered column definitions for this statement.
     */
    @Builder.Default
    private List<ImportColumn> columns = new ArrayList<>();

    /**
     * Optional statement-level lookup for non-CREATE operations. When present, it
     * replaces unique-column lookup for identifying existing rows.
     */
    @Builder.Default
    private List<ImportLookup> lookup = new ArrayList<>();

    /**
     * Optional store-specific query for non-CREATE operations. When present, it
     * replaces unique-column and lookup-based identification.
     */
    private ImportQuery query;

    /**
     * Rows to process using the statement's columns and operation.
     */
    @Builder.Default
    private List<ImportRow> rows = new ArrayList<>();

    /**
     * Extension bag for parser/module-specific statement metadata.
     */
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
