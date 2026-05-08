package com.coredeux.core.definition;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Entity-local module configuration.
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public final class CoredeuxModuleDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final boolean enabled;
    private final List<String> handlers;
    private final Object config;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfigMap() {
        return config instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
