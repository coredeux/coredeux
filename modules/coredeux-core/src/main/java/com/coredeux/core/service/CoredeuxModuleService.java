package com.coredeux.core.service;

/**
 * Public framework service for externally invoking configured modules on an
 * entity without going through a CRUD operation.
 */
public interface CoredeuxModuleService {

    /**
     * Executes all enabled modules configured for the given entity.
     *
     * @param entity the target entity
     * @param phase the phase to invoke
     * @param operation the lifecycle operation to expose in the context
     * @param <T> the entity type
     */
    <T> void executeAll(T entity, String phase, String operation);

    /**
     * Executes a specific enabled module configured for the given entity.
     *
     * @param entity the target entity
     * @param moduleName the configured module name
     * @param phase the phase to invoke
     * @param operation the lifecycle operation to expose in the context
     * @param <T> the entity type
     */
    <T> void executeModule(T entity, String moduleName, String phase, String operation);
}
