package com.coredeux.core.module.impl;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.util.CoredeuxGenericTypeResolver;

/**
 * Executes configured lifecycle hook beans for the current strategy phase.
 */
public class HooksModuleHandler implements CoredeuxEntityModuleHandler {

    private final CoredeuxComponentRegistry componentRegistry;

    public HooksModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    @Override
    public String getModuleName() {
        return "hooks";
    }

    @Override
    public <T> void execute(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition,
            String phase, OperationContext context) {
        for (String hookName : moduleDefinition.getHandlers()) {
            if (hookName == null || hookName.isBlank()) {
                continue;
            }

            CoredeuxEntityHook<T> hook;
            try {
                hook = resolveHook(hookName.trim(), entity, definition);
            } catch (CoredeuxStrategyException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new CoredeuxStrategyException(
                        "Unable to resolve hook bean: " + hookName + " for class: " + definition.getFullClassName(),
                        exception);
            }

            invokeHook(hook, entity, definition, phase, context);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> CoredeuxEntityHook<T> resolveHook(String hookName, T entity, CoredeuxEntityDefinition definition) {
        CoredeuxEntityHook<?> hook = componentRegistry.getComponent(hookName, CoredeuxEntityHook.class);
        validateSupportedType(hookName, hook.getClass(), entity, definition);
        return (CoredeuxEntityHook<T>) hook;
    }

    private <T> void validateSupportedType(String beanName, Class<?> beanType, T entity,
            CoredeuxEntityDefinition definition) {
        Class<?> supportedType = CoredeuxGenericTypeResolver.resolveFirstGeneric(beanType, CoredeuxEntityHook.class);
        if (supportedType == null || entity == null || supportedType.isAssignableFrom(entity.getClass())) {
            return;
        }
        throw new CoredeuxStrategyException("Configured hook bean '" + beanName + "' does not support entity type "
                + entity.getClass().getName() + " for class: " + definition.getFullClassName());
    }

    private <T> void invokeHook(CoredeuxEntityHook<T> hook, T entity, CoredeuxEntityDefinition definition, String phase,
            OperationContext context) {
        switch (phase) {
            case CoredeuxHookPhases.LOAD:
                hook.onLoad(entity, definition, context);
                break;
            case CoredeuxHookPhases.BEFORE_SAVE:
                hook.beforeSave(entity, definition, context);
                break;
            case CoredeuxHookPhases.AFTER_SAVE:
                hook.afterSave(entity, definition, context);
                break;
            case CoredeuxHookPhases.BEFORE_UPDATE:
                hook.beforeUpdate(entity, definition, context);
                break;
            case CoredeuxHookPhases.AFTER_UPDATE:
                hook.afterUpdate(entity, definition, context);
                break;
            case CoredeuxHookPhases.BEFORE_DELETE:
                hook.beforeDelete(entity, definition, context);
                break;
            case CoredeuxHookPhases.BEFORE_REFRESH:
                hook.beforeRefresh(entity, definition, context);
                break;
            case CoredeuxHookPhases.AFTER_REFRESH:
                hook.afterRefresh(entity, definition, context);
                break;
            default:
                throw new CoredeuxStrategyException("Unsupported hook phase: " + phase);
        }
    }
}
