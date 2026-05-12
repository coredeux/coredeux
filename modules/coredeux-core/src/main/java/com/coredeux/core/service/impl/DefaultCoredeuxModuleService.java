package com.coredeux.core.service.impl;

import java.util.List;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.resolver.EntityDataAccessResolver;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;
import com.coredeux.core.service.CoredeuxModuleService;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;
import com.coredeux.core.strategy.impl.AbstractCoredeuxStrategy;

/**
 * Default framework service that invokes configured entity modules outside the
 * CRUD strategy flow.
 */
public class DefaultCoredeuxModuleService extends AbstractCoredeuxStrategy implements CoredeuxModuleService {

    public DefaultCoredeuxModuleService(EntityDefinitionRegistry entityDefinitionRegistry,
            EntityDataAccessResolver entityDataAccessResolver, CoredeuxComponentRegistry componentRegistry,
            CoredeuxReflectionHelperService reflectionHelperService,
            CoredeuxRequestContextResolver requestContextResolver,
            List<CoredeuxEntityModuleHandler> moduleHandlers) {
        super(entityDefinitionRegistry, entityDataAccessResolver, componentRegistry, reflectionHelperService,
                requestContextResolver, moduleHandlers);
    }

    @Override
    public <T> void executeAll(T entity, String phase, String operation) {
        validateRequest(entity, phase, operation);
        CoredeuxEntityDefinition definition = getDefinition(entity);
        OperationContext context = buildOperationContext(entity, definition, operation);
        executeModules(resolveExecutionEntity(entity, context), definition, phase, context);
    }

    @Override
    public <T> void executeModule(T entity, String moduleName, String phase, String operation) {
        validateRequest(entity, phase, operation);
        if (moduleName == null || moduleName.isBlank()) {
            throw new CoredeuxValidationException("Module name is required for external module execution");
        }

        CoredeuxEntityDefinition definition = getDefinition(entity);
        CoredeuxModuleDefinition moduleDefinition = definition.getModuleDefinition(moduleName.trim())
                .filter(CoredeuxModuleDefinition::isEnabled)
                .orElseThrow(() -> new CoredeuxValidationException(
                        "No enabled module named '" + moduleName + "' configured for class: "
                                + definition.getFullClassName()));
        OperationContext context = buildOperationContext(entity, definition, operation);
        executeModule(resolveExecutionEntity(entity, context), definition, moduleDefinition, phase, context);
    }

    @SuppressWarnings("unchecked")
    private <T> OperationContext buildOperationContext(T entity, CoredeuxEntityDefinition definition, String operation) {
        Object identifier = requiresIdentifier(operation)
                ? requireIdentifier(entity, definition, operation)
                : extractIdentifier(entity, definition);
        T existing = requiresExistingState(operation)
                ? (strictExistingState(operation)
                        ? requireExistingEntity((Class<T>) entity.getClass(), identifier, operation)
                        : loadExistingEntity((Class<T>) entity.getClass(), identifier))
                : null;
        T newValue = CoredeuxLifecycleOperations.DELETE.equals(operation) ? null : entity;
        return createOperationContext(operation, identifier, existing, newValue);
    }

    @SuppressWarnings("unchecked")
    private <T> T resolveExecutionEntity(T entity, OperationContext context) {
        if (!CoredeuxLifecycleOperations.DELETE.equals(context.getLifecycleContext().getOperation())) {
            return entity;
        }

        Object oldValue = context.getLifecycleContext().getOldValue();
        return oldValue == null ? entity : (T) oldValue;
    }

    private boolean requiresIdentifier(String operation) {
        return CoredeuxLifecycleOperations.MODIFY.equals(operation)
                || CoredeuxLifecycleOperations.DELETE.equals(operation);
    }

    private boolean requiresExistingState(String operation) {
        return CoredeuxLifecycleOperations.UPSERT.equals(operation)
                || CoredeuxLifecycleOperations.MODIFY.equals(operation)
                || CoredeuxLifecycleOperations.DELETE.equals(operation);
    }

    private boolean strictExistingState(String operation) {
        return CoredeuxLifecycleOperations.MODIFY.equals(operation)
                || CoredeuxLifecycleOperations.DELETE.equals(operation);
    }

    private <T> void validateRequest(T entity, String phase, String operation) {
        if (entity == null) {
            throw new CoredeuxValidationException("Entity is required for external module execution");
        }
        if (phase == null || phase.isBlank()) {
            throw new CoredeuxValidationException("Phase is required for external module execution");
        }
        if (operation == null || operation.isBlank()) {
            throw new CoredeuxValidationException("Operation is required for external module execution");
        }
    }
}
