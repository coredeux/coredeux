package com.coredeux.impex.handler;

import java.util.Map;

import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.model.ImportMacro;
import com.coredeux.impex.model.ImportRow;
import com.coredeux.impex.model.ImportStatement;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImportValueContext {

    /**
     * Original value exactly as supplied in the row map.
     */
    Object rawValue;

    /**
     * String value after simple preprocessing such as default value and macro
     * replacement.
     */
    String effectiveValue;

    /**
     * Type expected by the target entity field.
     */
    Class<?> expectedType;

    /**
     * Element type for collection targets, when it can be resolved.
     */
    Class<?> collectionElementType;

    /**
     * Value type for map targets, when it can be resolved.
     */
    Class<?> mapValueType;

    /**
     * Target entity Java type.
     */
    Class<?> targetEntityType;

    /**
     * Column definition currently being processed.
     */
    ImportColumn column;

    /**
     * Row currently being processed, including all column values.
     */
    ImportRow row;

    /**
     * Statement/header currently being processed.
     */
    ImportStatement statement;

    /**
     * Request macros available to custom handlers.
     */
    Map<String, ImportMacro> macros;

    /**
     * Import row keys resolved during this execution.
     */
    Map<String, String> references;

}
