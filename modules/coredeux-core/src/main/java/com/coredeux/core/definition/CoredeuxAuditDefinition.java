package com.coredeux.core.definition;

import java.io.Serializable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Audit configuration for an entity definition.
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public final class CoredeuxAuditDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean enabled;
    private final String handler;
}
