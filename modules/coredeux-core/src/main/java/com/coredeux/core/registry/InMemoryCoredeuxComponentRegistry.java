package com.coredeux.core.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import com.coredeux.core.exceptions.CoredeuxStrategyException;

/**
 * Plain Java component registry for applications that wire Coredeux without a
 * dependency injection container.
 */
public class InMemoryCoredeuxComponentRegistry implements CoredeuxComponentRegistry {

    private final Map<String, Object> components;

    public InMemoryCoredeuxComponentRegistry(Map<String, ?> components) {
        this.components = components == null ? Map.of() : Map.copyOf(components);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> T getComponent(String name, Class<T> type) {
        Object component = components.get(name);
        if (component == null) {
            throw new CoredeuxStrategyException("Unable to resolve Coredeux component: " + name);
        }
        if (!type.isInstance(component)) {
            throw new CoredeuxStrategyException("Coredeux component '" + name + "' does not implement "
                    + type.getName() + ": " + component.getClass().getName());
        }
        return type.cast(component);
    }

    public static final class Builder {

        private final Map<String, Object> components = new LinkedHashMap<>();

        public Builder component(String name, Object component) {
            if (name == null || name.isBlank()) {
                throw new CoredeuxStrategyException("Coredeux component name is required");
            }
            if (component == null) {
                throw new CoredeuxStrategyException("Coredeux component is required for name: " + name);
            }
            components.put(name, component);
            return this;
        }

        public InMemoryCoredeuxComponentRegistry build() {
            return new InMemoryCoredeuxComponentRegistry(components);
        }
    }
}
