package com.coredeux.demo.workflow;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;

@Component
public class WorkflowsModuleHandler implements CoredeuxEntityModuleHandler {

    private static final String MODULE_NAME = "workflows";
    private static final String PHASES_CONFIG_KEY = "phases";

    private final ApplicationContext applicationContext;

    public WorkflowsModuleHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    public <T> void execute(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition,
            String phase, OperationContext context) {
        if (!shouldRun(moduleDefinition, phase)) {
            return;
        }

        for (String handlerName : moduleDefinition.getHandlers()) {
            if (handlerName == null || handlerName.isBlank()) {
                continue;
            }

            CoredeuxDemoWorkflowHandler<T> handler = resolveWorkflowHandler(handlerName.trim(), entity, definition);
            handler.execute(entity, definition, context);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> CoredeuxDemoWorkflowHandler<T> resolveWorkflowHandler(String handlerName, T entity,
            CoredeuxEntityDefinition definition) {
        try {
            CoredeuxDemoWorkflowHandler<?> handler = applicationContext.getBean(handlerName,
                    CoredeuxDemoWorkflowHandler.class);
            validateSupportedType(handlerName, handler.getClass(), entity, definition);
            return (CoredeuxDemoWorkflowHandler<T>) handler;
        } catch (CoredeuxStrategyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CoredeuxStrategyException("Unable to resolve workflow handler bean: " + handlerName
                    + " for class: " + definition.getFullClassName(), exception);
        }
    }

    private <T> void validateSupportedType(String beanName, Class<?> beanType, T entity,
            CoredeuxEntityDefinition definition) {
        Class<?> supportedType = ResolvableType.forClass(beanType)
                .as(CoredeuxDemoWorkflowHandler.class)
                .resolveGeneric(0);
        if (supportedType == null || entity == null || supportedType.isAssignableFrom(entity.getClass())) {
            return;
        }
        throw new CoredeuxStrategyException("Configured workflow handler bean '" + beanName
                + "' does not support entity type " + entity.getClass().getName()
                + " for class: " + definition.getFullClassName());
    }

    private boolean shouldRun(CoredeuxModuleDefinition moduleDefinition, String phase) {
        Map<String, Object> configMap = moduleDefinition.getConfigMap();
        Object configuredPhases = configMap.get(PHASES_CONFIG_KEY);
        if (configuredPhases == null) {
            return true;
        }
        if (!(configuredPhases instanceof List<?> phases) || CollectionUtils.isEmpty(phases)) {
            throw new CoredeuxStrategyException("Workflows module requires config.phases to be a non-empty list");
        }
        return phases.stream()
                .map(String::valueOf)
                .map(String::trim)
                .anyMatch(configuredPhase -> configuredPhase.equals(phase));
    }
}
