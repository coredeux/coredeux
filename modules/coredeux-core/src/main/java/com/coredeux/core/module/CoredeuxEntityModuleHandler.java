package com.coredeux.core.module;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;

/**
 * Strategy-facing contract for entity-level modules configured in YAML.
 */
public interface CoredeuxEntityModuleHandler {

    /**
     * @return the module name handled by this implementation
     */
    String getModuleName();

    /**
     * Executes the configured module for the given strategy phase.
     *
     * @param entity the current entity instance
     * @param definition the resolved entity definition
     * @param moduleDefinition the configured module definition
     * @param phase the current strategy phase
     * @param context the current operation context
     * @param <T> the entity type
     */
    <T> void execute(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition,
            String phase, OperationContext context);
}
