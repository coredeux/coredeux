package com.coredeux.core.strategy.impl;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.resolver.CoredeuxEntityDefinitionResolver;
import com.coredeux.core.resolver.EntityDataAccessResolver;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;
import com.coredeux.core.resolver.impl.DefaultCoredeuxEntityDefinitionResolver;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;
import com.coredeux.core.snapshot.CoredeuxEntitySnapshotService;
import com.coredeux.core.snapshot.impl.DefaultCoredeuxEntitySnapshotService;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;

/**
 * Base strategy implementation that resolves entity definitions, configured
 * modules, and data access services.
 */
public abstract class AbstractCoredeuxStrategy {

    private final EntityDefinitionRegistry entityDefinitionRegistry;
    private final EntityDataAccessResolver entityDataAccessResolver;
    private final CoredeuxComponentRegistry componentRegistry;
    private final CoredeuxReflectionHelperService reflectionHelperService;
    private final CoredeuxRequestContextResolver requestContextResolver;
    private final CoredeuxEntitySnapshotService entitySnapshotService;
    private final CoredeuxProperties coredeuxProperties;
    private final CoredeuxEntityDefinitionResolver entityDefinitionResolver;
    private final Map<String, CoredeuxEntityModuleHandler> moduleHandlers;

    protected AbstractCoredeuxStrategy(EntityDefinitionRegistry entityDefinitionRegistry,
            EntityDataAccessResolver entityDataAccessResolver, CoredeuxComponentRegistry componentRegistry,
            CoredeuxReflectionHelperService reflectionHelperService,
            CoredeuxRequestContextResolver requestContextResolver,
            CoredeuxProperties coredeuxProperties,
            CoredeuxEntitySnapshotService entitySnapshotService,
            CoredeuxEntityDefinitionResolver entityDefinitionResolver,
            List<CoredeuxEntityModuleHandler> moduleHandlers) {
        this.entityDefinitionRegistry = entityDefinitionRegistry;
        this.entityDataAccessResolver = entityDataAccessResolver;
        this.componentRegistry = componentRegistry;
        this.reflectionHelperService = reflectionHelperService;
        this.requestContextResolver = requestContextResolver;
        this.coredeuxProperties = coredeuxProperties == null ? new CoredeuxProperties() : coredeuxProperties;
        this.entitySnapshotService = entitySnapshotService == null
                ? new DefaultCoredeuxEntitySnapshotService()
                : entitySnapshotService;
        this.entityDefinitionResolver = entityDefinitionResolver == null
                ? new DefaultCoredeuxEntityDefinitionResolver(entityDefinitionRegistry, this.coredeuxProperties)
                : entityDefinitionResolver;
        this.moduleHandlers = toModuleHandlerMap(moduleHandlers);
    }

    protected <T> CoredeuxEntityDefinition getDefinition(Class<T> entityType) {
        return entityDefinitionResolver.resolve(entityType);
    }

    @SuppressWarnings("unchecked")
    protected <T> CoredeuxEntityDefinition getDefinition(T entity) {
        return getDefinition((Class<T>) entity.getClass());
    }

    protected <T> CoredeuxDataAccessService getDataAccessService(Class<T> entityType) {
        CoredeuxEntityDefinition definition = getDefinition(entityType);
        String beanName = resolveDataAccessService(definition);
        try {
            return componentRegistry.getComponent(beanName, CoredeuxDataAccessService.class);
        } catch (Exception exception) {
            throw new CoredeuxStrategyException(
                    "Unable to resolve CoredeuxDataAccessService bean: " + beanName + " for class: "
                            + entityType.getName(),
                    exception);
        }
    }

    protected String resolveDataAccessService(CoredeuxEntityDefinition definition) {
        String beanName = entityDataAccessResolver.resolveDataAccessService(definition);
        if (beanName != null && !beanName.isBlank()) {
            return beanName;
        }
        throw new CoredeuxValidationException(
                "No data access service configured for entity class: " + definition.getFullClassName());
    }

    protected CoredeuxComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    protected <T> void executeModules(T entity, CoredeuxEntityDefinition definition, String phase,
            OperationContext context) {
        if (definition == null || definition.getModules() == null || definition.getModules().isEmpty()) {
            return;
        }

        for (CoredeuxModuleDefinition moduleDefinition : definition.getModules()) {
            executeModule(entity, definition, moduleDefinition, phase, context);
        }
    }

    protected <T> void executeModule(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition,
            String phase, OperationContext context) {
        if (moduleDefinition == null || !moduleDefinition.isEnabled()) {
            return;
        }

        CoredeuxEntityModuleHandler handler = moduleHandlers.get(moduleDefinition.getName());
        if (handler == null) {
            return;
        }

        handler.execute(entity, definition, moduleDefinition, phase, context);
    }

    protected <T> OperationContext createOperationContext(String operationName, Object identifier, T oldValue,
            T newValue) {
        T oldSnapshot = entitySnapshotService.snapshot(oldValue);
        return OperationContext.builder()
                .invokedAt(Instant.now())
                .requestContext(requestContextResolver.resolve())
                .lifecycleContext(EntityLifecycleContext.<T>builder()
                        .operation(operationName)
                        .identifier(identifier)
                        .oldValue(oldSnapshot)
                        .newValue(newValue)
                        .build())
                .build();
    }

    protected <T> Object extractIdentifier(T entity, CoredeuxEntityDefinition definition) {
        if (entity == null || definition == null) {
            return null;
        }

        String identifierField = definition.getIdentifier();
        if (identifierField == null || identifierField.isBlank()) {
            return null;
        }

        Object value = reflectionHelperService.getFieldValue(identifierField, entity);
        if (value instanceof String stringValue && stringValue.isBlank()) {
            return null;
        }
        return value;
    }

    protected <T> Object requireIdentifier(T entity, CoredeuxEntityDefinition definition, String operationLabel) {
        Object identifier = extractIdentifier(entity, definition);
        if (identifier == null) {
            String identifierField = definition.getIdentifier();
            throw new CoredeuxValidationException(
                    "Missing identifier '" + (identifierField == null ? "id" : identifierField) + "' for "
                            + operationLabel + " on class: " + definition.getFullClassName());
        }
        return identifier;
    }


    protected <T> T loadExistingEntity(Class<T> entityType, Object identifier) {
        if (identifier == null) {
            return null;
        }
        return getDataAccessService(entityType).load(String.valueOf(identifier), entityType);
    }

    protected <T> T requireExistingEntity(Class<T> entityType, Object identifier, String operationLabel) {
        T existing = loadExistingEntity(entityType, identifier);
        if (existing == null) {
            throw new CoredeuxDataAccessException(
                    "No existing entity found for " + operationLabel + " with identifier '" + identifier
                            + "' on class: " + entityType.getName());
        }
        return existing;
    }

    protected <T> void invokeLoadModules(T entity, CoredeuxEntityDefinition definition, Object identifier) {
        if (entity == null) {
            return;
        }
        OperationContext fetchContext = createOperationContext(CoredeuxLifecycleOperations.FETCH,
                identifier != null ? identifier : extractIdentifier(entity, definition), null, entity);
        executeModules(entity, definition, CoredeuxHookPhases.LOAD, fetchContext);
    }

    protected <T> void invokeLoadModules(SearchResult<T> result, CoredeuxEntityDefinition definition) {
        if (result == null || result.getResults() == null) {
            return;
        }
        for (T entity : result.getResults()) {
            if (entity != null) {
                OperationContext fetchContext = createOperationContext(CoredeuxLifecycleOperations.FETCH,
                        extractIdentifier(entity, definition), null, entity);
                executeModules(entity, definition, CoredeuxHookPhases.LOAD, fetchContext);
            }
        }
    }

    private Map<String, CoredeuxEntityModuleHandler> toModuleHandlerMap(List<CoredeuxEntityModuleHandler> handlers) {
        Map<String, CoredeuxEntityModuleHandler> handlerMap = new LinkedHashMap<>();
        if (handlers == null) {
            return Map.of();
        }

        for (CoredeuxEntityModuleHandler handler : handlers) {
            CoredeuxEntityModuleHandler existing = handlerMap.putIfAbsent(handler.getModuleName(), handler);
            if (existing != null) {
                throw new CoredeuxValidationException(
                        "Duplicate Coredeux module handler found for module: " + handler.getModuleName());
            }
        }
        return Map.copyOf(handlerMap);
    }

}
