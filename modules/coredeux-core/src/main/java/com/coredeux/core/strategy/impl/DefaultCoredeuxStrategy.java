package com.coredeux.core.strategy.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.resolver.EntityDataAccessResolver;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;
import com.coredeux.core.strategy.CoredeuxStrategy;

/**
 * Default strategy implementation that resolves the configured data access
 * service from the entity definition registry and applies enabled modules.
 */
public class DefaultCoredeuxStrategy extends AbstractCoredeuxStrategy implements CoredeuxStrategy {

    public DefaultCoredeuxStrategy(EntityDefinitionRegistry entityDefinitionRegistry,
            EntityDataAccessResolver entityDataAccessResolver, CoredeuxComponentRegistry componentRegistry,
            CoredeuxReflectionHelperService reflectionHelperService,
            CoredeuxRequestContextResolver requestContextResolver,
            List<CoredeuxEntityModuleHandler> moduleHandlers) {
        super(entityDefinitionRegistry, entityDataAccessResolver, componentRegistry, reflectionHelperService,
                requestContextResolver, moduleHandlers);
    }

    @Override
    public <T> T load(String id, Class<T> type) {
        CoredeuxEntityDefinition definition = getDefinition(type);
        CoredeuxDataAccessService dataAccessService = getDataAccessService(type);
        T entity = dataAccessService.load(id, type);
        if (entity != null) {
            Object identifier = extractIdentifier(entity, definition);
            OperationContext fetchContext = createOperationContext(CoredeuxLifecycleOperations.FETCH,
                    identifier != null ? identifier : id, null, entity);
            executeModules(entity, definition, CoredeuxHookPhases.LOAD, fetchContext);
        }
        return entity;
    }

    @Override
    public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage) {
        CoredeuxEntityDefinition definition = getDefinition(type);
        CoredeuxDataAccessService dataAccessService = getDataAccessService(type);
        SearchResult<T> result = dataAccessService.query(query, params, type, pageSize, currentPage);
        invokeLoadModules(result, definition);
        return result;
    }

    @Override
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        CoredeuxEntityDefinition definition = getDefinition(type);
        CoredeuxDataAccessService dataAccessService = getDataAccessService(type);
        SearchResult<T> result = dataAccessService.loadAll(params, type, pageSize, currentPage);
        invokeLoadModules(result, definition);
        return result;
    }

    @Override
    public Set<String> supportedComparators(Class<?> type) {
        return getDataAccessService(type).supportedComparators(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> String save(T entity) {
        CoredeuxEntityDefinition definition = getDefinition(entity);
        Object identifier = extractIdentifier(entity, definition);
        T existing = loadExistingEntity((Class<T>) entity.getClass(), identifier);
        OperationContext beforeContext = createOperationContext(CoredeuxLifecycleOperations.CREATE, identifier,
                existing, entity);
        executeModules(entity, definition, CoredeuxHookPhases.BEFORE_SAVE, beforeContext);
        String persistedIdentifier = getDataAccessService(entity.getClass()).save(entity);
        Object resolvedIdentifier = persistedIdentifier != null && !persistedIdentifier.isBlank() ? persistedIdentifier
                : identifier;
        OperationContext afterContext = createOperationContext(CoredeuxLifecycleOperations.UPSERT,
                resolvedIdentifier, existing, entity);
        executeModules(entity, definition, CoredeuxHookPhases.AFTER_SAVE, afterContext);
        return persistedIdentifier;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void update(T entity) {
        CoredeuxEntityDefinition definition = getDefinition(entity);
        Object identifier = requireIdentifier(entity, definition, CoredeuxLifecycleOperations.MODIFY);
        T existing = requireExistingEntity((Class<T>) entity.getClass(), identifier, CoredeuxLifecycleOperations.MODIFY);
        OperationContext modifyContext = createOperationContext(CoredeuxLifecycleOperations.MODIFY, identifier,
                existing, entity);
        executeModules(entity, definition, CoredeuxHookPhases.BEFORE_UPDATE, modifyContext);
        getDataAccessService(entity.getClass()).update(entity);
        executeModules(entity, definition, CoredeuxHookPhases.AFTER_UPDATE, modifyContext);
    }

    @Override
    public <T> void remove(String id, Class<T> type) {
        T existing = requireExistingEntity(type, id, CoredeuxLifecycleOperations.DELETE);
        remove(existing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void remove(T entity) {
        CoredeuxEntityDefinition definition = getDefinition(entity);
        Object identifier = requireIdentifier(entity, definition, CoredeuxLifecycleOperations.DELETE);
        T existing = requireExistingEntity((Class<T>) entity.getClass(), identifier, CoredeuxLifecycleOperations.DELETE);
        OperationContext deleteContext = createOperationContext(CoredeuxLifecycleOperations.DELETE, identifier,
                existing, null);
        executeModules(existing, definition, CoredeuxHookPhases.BEFORE_DELETE, deleteContext);
        getDataAccessService(entity.getClass()).remove(entity);
    }

    @Override
    public <T> void refresh(T entity) {
        CoredeuxEntityDefinition definition = getDefinition(entity);
        Object identifier = extractIdentifier(entity, definition);
        OperationContext fetchContext = createOperationContext(CoredeuxLifecycleOperations.FETCH, identifier, null,
                entity);
        executeModules(entity, definition, CoredeuxHookPhases.BEFORE_REFRESH, fetchContext);
        getDataAccessService(entity.getClass()).refresh(entity);
        executeModules(entity, definition, CoredeuxHookPhases.AFTER_REFRESH, fetchContext);
    }

}
