package com.coredeux.core.testsupport;

import java.util.LinkedHashMap;
import java.util.Map;

import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.registry.CoredeuxComponentRegistry;

public class TestComponentRegistry implements CoredeuxComponentRegistry {

    private final Map<String, Object> components = new LinkedHashMap<>();

    public void registerSingleton(String name, Object component) {
        components.put(name, component);
    }

    @Override
    public <T> T getComponent(String name, Class<T> type) {
        Object component = components.get(name);
        if (component == null) {
            throw new CoredeuxStrategyException("Missing component: " + name);
        }
        if (!type.isInstance(component)) {
            throw new CoredeuxStrategyException("Component '" + name + "' does not implement " + type.getName());
        }
        return type.cast(component);
    }
}
