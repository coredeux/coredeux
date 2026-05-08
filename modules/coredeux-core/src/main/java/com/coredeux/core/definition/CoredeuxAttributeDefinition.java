package com.coredeux.core.definition;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Attribute-level metadata for an entity definition.
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public final class CoredeuxAttributeDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String type;
    private final boolean required;
    private final boolean searchable;
    private final List<String> validators;
}
