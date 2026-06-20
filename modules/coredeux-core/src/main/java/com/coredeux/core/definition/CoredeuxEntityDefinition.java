package com.coredeux.core.definition;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Entity-level metadata used by Coredeux to resolve persistence and module-based
 * behavior.
 */
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public final class CoredeuxEntityDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fullClassName;
    private final String name;
    private final String identifier;
    private final CoredeuxStorageDefinition storage;
    private final List<CoredeuxModuleDefinition> modules;

    public CoredeuxModuleDefinition getAudit() {
        return getModuleDefinition("audit").orElse(null);
    }

    public List<String> getValidators() {
        return getEnabledHandlers("validators");
    }

    public List<String> getHooks() {
        return getEnabledHandlers("hooks");
    }

    @SuppressWarnings("unchecked")
    public List<CoredeuxAttributeDefinition> getAttributes() {
        return getModuleDefinition("attributes")
                .filter(CoredeuxModuleDefinition::isEnabled)
                .map(CoredeuxModuleDefinition::getConfig)
                .filter(List.class::isInstance)
                .map(value -> (List<CoredeuxAttributeDefinition>) value)
                .orElse(List.of());
    }

    public Object getModule(String moduleName) {
        return getModuleDefinition(moduleName).orElse(null);
    }

    public Optional<CoredeuxModuleDefinition> getModuleDefinition(String moduleName) {
        if (modules == null) {
            return Optional.empty();
        }
        return modules.stream()
                .filter(module -> moduleName.equals(module.getName()))
                .findFirst();
    }

    private List<String> getEnabledHandlers(String moduleName) {
        return getModuleDefinition(moduleName)
                .filter(CoredeuxModuleDefinition::isEnabled)
                .map(CoredeuxModuleDefinition::getHandlers)
                .orElse(List.of());
    }
}
