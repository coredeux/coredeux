package com.coredeux.core.definition;

import java.io.Serializable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Storage and data access binding for an entity definition.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public final class CoredeuxStorageDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String store;
    private final String identifier;
    private final String dataAccessService;
}
