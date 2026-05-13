package com.coredeux.demo.workflow;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;

public class WorkflowsModuleHandler implements CoredeuxEntityModuleHandler {

    private static final String MODULE_NAME = "workflows";
    private static final String PHASES_CONFIG_KEY = "phases";

    private final CoredeuxComponentRegistry componentRegistry;

    public WorkflowsModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        this.componentRegistry = Objects.requireNonNull(componentRegistry, "componentRegistry");
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
            resolveWorkflowHandler(handlerName.trim()).execute(entity, definition, context);
        }
    }

    private CoredeuxDemoWorkflowHandler<Object> resolveWorkflowHandler(String handlerName) {
        try {
            @SuppressWarnings("unchecked")
            CoredeuxDemoWorkflowHandler<Object> handler = componentRegistry.getComponent(handlerName,
                    CoredeuxDemoWorkflowHandler.class);
            return handler;
        } catch (Exception exception) {
            throw new CoredeuxStrategyException("Unable to resolve workflow handler component: " + handlerName,
                    exception);
        }
    }

    private boolean shouldRun(CoredeuxModuleDefinition moduleDefinition, String phase) {
        Map<String, Object> configMap = moduleDefinition.getConfigMap();
        Object configuredPhases = configMap.get(PHASES_CONFIG_KEY);
        if (configuredPhases == null) {
            return true;
        }
        if (!(configuredPhases instanceof List<?> phases) || phases.isEmpty()) {
            throw new CoredeuxStrategyException("Workflows module requires config.phases to be a non-empty list");
        }
        return phases.stream()
                .map(String::valueOf)
                .map(String::trim)
                .anyMatch(configuredPhase -> configuredPhase.equals(phase));
    }
}
