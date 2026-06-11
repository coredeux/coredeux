package com.coredeux.drl.core.module.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.module.impl.HooksModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;

/**
 * Executes configured lifecycle hook beans for the current strategy phase.
 */
public class DRLHooksModuleHandler extends HooksModuleHandler implements CoredeuxEntityModuleHandler {

    private static final String DRL_SUFFIX = ".drl";
    private static final String DRL_SERVICE_BEAN_NAME = "coredeuxDrlService";

    private final CoredeuxComponentRegistry componentRegistry;

    public DRLHooksModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        super(componentRegistry);
        this.componentRegistry = componentRegistry;
    }

    @Override
    public String getModuleName() {
        return "hooks";
    }

    @Override
    public <T> void execute(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition,
            String phase, OperationContext context) {
        if (moduleDefinition.getHandlers() != null) {
            for (String hookName : moduleDefinition.getHandlers()) {
                if (StringUtils.isBlank(hookName)) {
                    continue;
                }

                String trimmedName = hookName.trim();
                if (isDrlHandler(trimmedName)) {
                    executeDrlHook(trimmedName, entity, definition, phase, context);
                    continue;
                }

                executeJavaHook(trimmedName, entity, definition, phase, context);
            }
        }
    }

    private <T> void executeDrlHook(String hookName, T entity, CoredeuxEntityDefinition definition, String phase,
            OperationContext context) {
        String method = phaseToHookMethod(phase);
        if (method == null) {
            return;
        }

        RuleContext<Void> ruleContext = RuleContext.<Void>method(method)
                .param("entity", entity)
                .param("definition", definition)
                .param("context", context)
                .param("phase", phase)
                .fact(entity);
        try {
            drlService().execute(hookName, ruleContext);
        } catch (CoredeuxStrategyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CoredeuxStrategyException(
                    "Unable to execute DRL hook: " + hookName + " for class: " + definition.getFullClassName(),
                    exception);
        }
    }

    private <T> void executeJavaHook(String hookName, T entity, CoredeuxEntityDefinition definition, String phase,
            OperationContext context) {
        try {
            CoredeuxEntityHook<T> hook = resolveHook(hookName, entity, definition);
            invokeHook(hook, entity, definition, phase, context);
        } catch (CoredeuxStrategyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CoredeuxStrategyException(
                    "Unable to resolve hook bean: " + hookName + " for class: " + definition.getFullClassName(),
                    exception);
        }
    }

    private String phaseToHookMethod(String phase) {
        switch (phase) {
            case CoredeuxHookPhases.LOAD:
                return "onLoad";
            case CoredeuxHookPhases.BEFORE_SAVE:
                return "beforeSave";
            case CoredeuxHookPhases.AFTER_SAVE:
                return "afterSave";
            case CoredeuxHookPhases.BEFORE_UPDATE:
                return "beforeUpdate";
            case CoredeuxHookPhases.AFTER_UPDATE:
                return "afterUpdate";
            case CoredeuxHookPhases.BEFORE_DELETE:
                return "beforeDelete";
            case CoredeuxHookPhases.BEFORE_REFRESH:
                return "beforeRefresh";
            case CoredeuxHookPhases.AFTER_REFRESH:
                return "afterRefresh";
            default:
                return null;
        }
    }

    private boolean isDrlHandler(String handlerName) {
        return StringUtils.isNotBlank(handlerName) && handlerName.endsWith(DRL_SUFFIX);
    }

    private DRLService drlService() {
        return componentRegistry.getComponent(DRL_SERVICE_BEAN_NAME, DRLService.class);
    }

}
