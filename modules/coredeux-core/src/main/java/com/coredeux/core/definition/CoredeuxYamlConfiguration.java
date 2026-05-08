package com.coredeux.core.definition;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Root YAML-backed Coredeux configuration payload.
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public final class CoredeuxYamlConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<CoredeuxEntityDefinition> entities;
}
