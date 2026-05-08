package com.coredeux.core.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;

/**
 * In-memory registry implementation for YAML-loaded entity definitions.
 */
public class InMemoryEntityDefinitionRegistry implements EntityDefinitionRegistry {

    private final Map<String, CoredeuxEntityDefinition> definitions;

    public InMemoryEntityDefinitionRegistry(List<CoredeuxEntityDefinition> definitions) {
        Map<String, CoredeuxEntityDefinition> definitionMap = new LinkedHashMap<>();
        for (CoredeuxEntityDefinition definition : definitions) {
            CoredeuxEntityDefinition existing = definitionMap.putIfAbsent(definition.getFullClassName(), definition);
            if (existing != null) {
                throw new CoredeuxValidationException(
                        "Duplicate entity definition found for class: " + definition.getFullClassName());
            }
        }
        this.definitions = Collections.unmodifiableMap(definitionMap);
    }

    @Override
    public Optional<CoredeuxEntityDefinition> findByFullClassName(String fullClassName) {
        return Optional.ofNullable(definitions.get(fullClassName));
    }

    @Override
    public Collection<CoredeuxEntityDefinition> getAll() {
        return definitions.values();
    }
}
