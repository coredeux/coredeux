package com.coredeux.core.resolver;

import com.coredeux.core.definition.CoredeuxEntityDefinition;

/**
 * Resolves the configured data access service name for an entity definition.
 */
public interface EntityDataAccessResolver {

    /**
     * Resolves the configured data access service name for the given entity
     * definition.
     *
     * @param definition the entity definition
     * @return the configured data access service name
     */
    String resolveDataAccessService(CoredeuxEntityDefinition definition);
}
