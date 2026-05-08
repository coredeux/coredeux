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
public class ImportColumn {

    /**
     * Column name used by row values and the target entity field.
     */
    private String name;

    /**
     * Marks this column as part of the lookup key for MODIFY, UPSERT, DELETE, and
     * FETCH operations.
     */
    private boolean unique;

    /**
     * Allows a unique lookup to search for null when the resolved column value is
     * null.
     */
    private boolean nullSearch;

    /**
     * Value used when the row omits this column or supplies a blank value.
     */
    private String defaultValue;

    /**
     * Collection update mode. POJO targets support replace, which overwrites the
     * collection value, append, which keeps existing values and adds incoming
     * values, and clear, which empties or initializes the collection.
     */
    @Builder.Default
    private String mode = "replace";

    /**
     * Reference lookup instruction. Examples: code, catalogue:version, or * for a
     * previously stored row key reference.
     */
    private String reference;

    /**
     * Optional Spring bean name/id of a custom value handler. When omitted, the
     * default type-aware handler is used.
     */
    private String handler;

    /**
     * Extension bag for parser/module-specific column metadata without changing the
     * core contract.
     */
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();

}
