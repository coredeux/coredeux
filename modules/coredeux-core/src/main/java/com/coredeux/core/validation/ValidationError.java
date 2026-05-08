package com.coredeux.core.validation;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single validation error for a field or entity-level rule.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationError implements Serializable {

    private static final long serialVersionUID = 1L;

    private String field;
    private String message;
}
