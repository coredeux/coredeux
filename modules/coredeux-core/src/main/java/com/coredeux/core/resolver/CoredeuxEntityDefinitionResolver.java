package com.coredeux.core.resolver;

import com.coredeux.core.definition.CoredeuxEntityDefinition;

/**
 * Resolves the effective entity definition for a runtime entity type.
 */
public interface CoredeuxEntityDefinitionResolver {

    /**
     * Resolves the effective definition for the given entity class.
     *
     * @param entityType the entity class
     * @param <T> the entity type
     * @return the configured or synthesized definition
     */
    <T> CoredeuxEntityDefinition resolve(Class<T> entityType);

    /**
     * Resolves the effective definition for the given entity instance.
     *
     * @param entity the entity instance
     * @param <T> the entity type
     * @return the configured or synthesized definition
     */
    @SuppressWarnings("unchecked")
    default <T> CoredeuxEntityDefinition resolve(T entity) {
        return resolve((Class<T>) entity.getClass());
    }
}
