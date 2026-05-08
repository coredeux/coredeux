package com.coredeux.core.registry;

import java.util.Collection;
import java.util.Optional;

import com.coredeux.core.definition.CoredeuxEntityDefinition;

/**
 * Registry for entity definitions loaded from configuration.
 */
public interface EntityDefinitionRegistry {

    /**
     * Looks up an entity definition by fully qualified class name.
     *
     * @param fullClassName the entity class name
     * @return the resolved entity definition, if present
     */
    Optional<CoredeuxEntityDefinition> findByFullClassName(String fullClassName);

    /**
     * Looks up an entity definition for the given class.
     *
     * @param entityType the entity class
     * @param <T> the entity type
     * @return the resolved entity definition, if present
     */
    default <T> Optional<CoredeuxEntityDefinition> findByEntityType(Class<T> entityType) {
        return findByFullClassName(entityType.getName());
    }

    /**
     * Returns all configured entity definitions.
     *
     * @return all configured entity definitions
     */
    Collection<CoredeuxEntityDefinition> getAll();
}
