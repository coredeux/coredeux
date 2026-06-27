package com.coredeux.drl.core.strategy.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.resolver.CoredeuxEntityDefinitionResolver;
import com.coredeux.core.resolver.EntityDataAccessResolver;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;
import com.coredeux.core.strategy.CoredeuxStrategy;
import com.coredeux.core.strategy.impl.DefaultCoredeuxStrategy;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;
import com.coredeux.drl.support.RuleContextExecutionSupport;

/**
 * Default strategy implementation that resolves the configured data access
 * service from the entity definition registry and routes <code>.drl</code>
 * data-access targets through the DRL runtime.
 */
public class DefaultDRLCoredeuxStrategy extends DefaultCoredeuxStrategy implements CoredeuxStrategy {

    private static final String DRL_SUFFIX = ".drl";
    private static final String DRL_SERVICE_BEAN_NAME = "coredeuxDrlService";

    public DefaultDRLCoredeuxStrategy(EntityDefinitionRegistry entityDefinitionRegistry,
            EntityDataAccessResolver entityDataAccessResolver, CoredeuxComponentRegistry componentRegistry,
            CoredeuxReflectionHelperService reflectionHelperService,
            CoredeuxRequestContextResolver requestContextResolver, CoredeuxProperties coredeuxProperties,
            CoredeuxEntityDefinitionResolver entityDefinitionResolver,
            List<CoredeuxEntityModuleHandler> moduleHandlers) {
        super(entityDefinitionRegistry, entityDataAccessResolver, componentRegistry, reflectionHelperService,
                requestContextResolver, coredeuxProperties, entityDefinitionResolver, moduleHandlers);
    }

    @Override
    public <T> T load(String id, Class<T> type) {
        CoredeuxEntityDefinition definition = getDefinition(type);
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            T entity = executeDrlLoad(beanName, id, type);
            if (entity != null) {
                invokeLoadModules(entity, definition, id);
            }
            return entity;
        }
        return super.load(id, type);
    }

    @Override
    public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage) {
        CoredeuxEntityDefinition definition = getDefinition(type);
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            RuleContext<SearchResult<T>> context = RuleContext.<SearchResult<T>>method("query")
                    .param("query", query)
                    .param("params", params)
                    .param("type", type)
                    .param("pageSize", pageSize)
                    .param("currentPage", currentPage);
            SearchResult<T> result = executeDrl(beanName, context);
            invokeLoadModules(result, definition);
            return result;
        }
        return super.query(query, params, type, pageSize, currentPage);
    }

    @Override
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        CoredeuxEntityDefinition definition = getDefinition(type);
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            RuleContext<SearchResult<T>> context = RuleContext.<SearchResult<T>>method("loadAll")
                    .param("params", params)
                    .param("type", type)
                    .param("pageSize", pageSize)
                    .param("currentPage", currentPage);
            SearchResult<T> result = executeDrl(beanName, context);
            invokeLoadModules(result, definition);
            return result;
        }
        return super.loadAll(params, type, pageSize, currentPage);
    }

    @Override
    public Set<String> supportedComparators(Class<?> type) {
        CoredeuxEntityDefinition definition = getDefinition(type);
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            RuleContext<Set<String>> context = RuleContext.<Set<String>>method("supportedComparators")
                    .param("type", type);
            Set<String> comparators = executeDrl(beanName, context);
            return comparators != null ? comparators : super.supportedComparators(type);
        }
        return super.supportedComparators(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> String save(T entity) {
        CoredeuxEntityDefinition definition = getDefinition(entity);
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            Object identifier = extractIdentifier(entity, definition);
            T existing = loadExistingEntity(definition, (Class<T>) entity.getClass(), identifier);
            OperationContext beforeContext = createOperationContext(CoredeuxLifecycleOperations.CREATE, identifier,
                    existing, entity);
            executeModules(entity, definition, CoredeuxHookPhases.BEFORE_SAVE, beforeContext);
            RuleContext<String> context = RuleContext.<String>method("save")
                    .param("entity", entity)
                    .param("identifier", identifier)
                    .param("existing", existing)
                    .param("type", entity.getClass())
                    .fact(entity);
            String persistedIdentifier = executeDrl(beanName, context);
            Object resolvedIdentifier = persistedIdentifier != null && !persistedIdentifier.isBlank()
                    ? persistedIdentifier
                    : identifier;
            OperationContext afterContext = createOperationContext(CoredeuxLifecycleOperations.UPSERT,
                    resolvedIdentifier, existing, entity);
            executeModules(entity, definition, CoredeuxHookPhases.AFTER_SAVE, afterContext);
            return persistedIdentifier;
        }
        return super.save(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void update(T entity) {
        CoredeuxEntityDefinition definition = getDefinition(entity);
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            Object identifier = requireIdentifier(entity, definition, CoredeuxLifecycleOperations.MODIFY);
            T existing = requireExistingEntity(definition, (Class<T>) entity.getClass(), identifier,
                    CoredeuxLifecycleOperations.MODIFY);
            OperationContext modifyContext = createOperationContext(CoredeuxLifecycleOperations.MODIFY, identifier,
                    existing, entity);
            executeModules(entity, definition, CoredeuxHookPhases.BEFORE_UPDATE, modifyContext);
            RuleContext<Void> context = RuleContext.<Void>method("update")
                    .param("entity", entity)
                    .param("identifier", identifier)
                    .param("existing", existing)
                    .param("type", entity.getClass())
                    .fact(entity);
            executeDrl(beanName, context);
            executeModules(entity, definition, CoredeuxHookPhases.AFTER_UPDATE, modifyContext);
            return;
        }
        super.update(entity);
    }

    @Override
    public <T> void remove(String id, Class<T> type) {
        CoredeuxEntityDefinition definition = getDefinition(type);
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            T existing = requireExistingEntity(definition, type, id, CoredeuxLifecycleOperations.DELETE);
            removeResolvedEntity(definition, existing, id);
            return;
        }
        super.remove(id, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void remove(T entity) {
        CoredeuxEntityDefinition definition = getDefinition(entity);
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            Object identifier = requireIdentifier(entity, definition, CoredeuxLifecycleOperations.DELETE);
            T existing = requireExistingEntity(definition, (Class<T>) entity.getClass(), identifier,
                    CoredeuxLifecycleOperations.DELETE);
            removeResolvedEntity(definition, existing, identifier);
            return;
        }
        super.remove(entity);
    }

    @Override
    public <T> void refresh(T entity) {
        CoredeuxEntityDefinition definition = getDefinition(entity);
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            Object identifier = extractIdentifier(entity, definition);
            OperationContext fetchContext = createOperationContext(CoredeuxLifecycleOperations.FETCH, identifier, null,
                    entity);
            executeModules(entity, definition, CoredeuxHookPhases.BEFORE_REFRESH, fetchContext);
            RuleContext<Void> context = RuleContext.<Void>method("refresh")
                    .param("entity", entity)
                    .param("identifier", identifier)
                    .param("type", entity.getClass())
                    .fact(entity);
            executeDrl(beanName, context);
            executeModules(entity, definition, CoredeuxHookPhases.AFTER_REFRESH, fetchContext);
            return;
        }
        super.refresh(entity);
    }

    private boolean isDrlDataAccess(String beanName) {
        return StringUtils.isNotBlank(beanName) && beanName.endsWith(DRL_SUFFIX);
    }

    private DRLService drlService() {
        try {
            return getComponentRegistry().getComponent(DRL_SERVICE_BEAN_NAME, DRLService.class);
        } catch (RuntimeException exception) {
            throw new CoredeuxStrategyException(
                    "Unable to resolve Coredeux DRL service bean: " + DRL_SERVICE_BEAN_NAME, exception);
        }
    }

    private <T> T executeDrl(String ruleId, RuleContext<T> context) {
        drlService().execute(ruleId, context);
        return RuleContextExecutionSupport.outputOrThrow(context, "DRL data-access rule '" + ruleId + "'");
    }

    private <T> T executeDrlLoad(String ruleId, Object identifier, Class<T> type) {
        RuleContext<T> context = RuleContext.<T>method("load")
                .param("id", identifier)
                .param("identifier", identifier)
                .param("type", type);
        return executeDrl(ruleId, context);
    }

    private <T> T loadExistingEntity(CoredeuxEntityDefinition definition, Class<T> entityType, Object identifier) {
        if (identifier == null) {
            return null;
        }
        String beanName = resolveDataAccessService(definition);
        if (isDrlDataAccess(beanName)) {
            return executeDrlLoad(beanName, identifier, entityType);
        }
        return super.loadExistingEntity(entityType, identifier);
    }

    private <T> T requireExistingEntity(CoredeuxEntityDefinition definition, Class<T> entityType, Object identifier,
            String operationLabel) {
        T existing = loadExistingEntity(definition, entityType, identifier);
        if (existing == null) {
            throw new CoredeuxDataAccessException(
                    "No existing entity found for " + operationLabel + " with identifier '" + identifier
                            + "' on class: " + entityType.getName());
        }
        return existing;
    }

    private <T> void removeResolvedEntity(CoredeuxEntityDefinition definition, T existing, Object identifier) {
        if (existing == null) {
            return;
        }
        OperationContext deleteContext = createOperationContext(CoredeuxLifecycleOperations.DELETE, identifier, existing,
                null);
        executeModules(existing, definition, CoredeuxHookPhases.BEFORE_DELETE, deleteContext);
        RuleContext<Void> context = RuleContext.<Void>method("remove")
                .param("entity", existing)
                .param("identifier", identifier)
                .param("type", existing.getClass())
                .fact(existing);
        String beanName = resolveDataAccessService(definition);
        drlService().execute(beanName, context);
        RuleContextExecutionSupport.throwIfException(context,
                "DRL data-access rule 'remove' for class: " + definition.getFullClassName());
    }

}
