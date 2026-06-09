package com.coredeux.core.module.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.coredeux.core.audit.CoredeuxEntityAuditHandler;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.util.CoredeuxGenericTypeResolver;

/**
 * Executes configured audit handlers for the configured write operations.
 */
public class AuditModuleHandler implements CoredeuxEntityModuleHandler {

    protected static final String MODULE_NAME = "audit";
    protected static final String ALL = "ALL";
    protected static final String SAVE = "SAVE";
    protected static final String UPDATE = "UPDATE";
    protected static final String DELETE = "DELETE";
    protected static final List<String> SUPPORTED_OPERATIONS = List.of(ALL, SAVE, UPDATE, DELETE);

    private final CoredeuxComponentRegistry componentRegistry;

    public AuditModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    public <T> void execute(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition,
            String phase, OperationContext context) {
        String operation = mapPhaseToAuditOperation(phase);
        if (operation == null) {
            return;
        }

        List<String> configuredOperations = resolveOperations(moduleDefinition, definition);
        if (!configuredOperations.contains(ALL) && !configuredOperations.contains(operation)) {
            return;
        }

        for (String auditHandlerName : moduleDefinition.getHandlers()) {
            if (auditHandlerName == null || auditHandlerName.isBlank()) {
                continue;
            }

            CoredeuxEntityAuditHandler<T> auditHandler;
            try {
                auditHandler = resolveAuditHandler(auditHandlerName.trim(), entity, definition);
            } catch (CoredeuxStrategyException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new CoredeuxStrategyException(
                        "Unable to resolve audit handler bean: " + auditHandlerName + " for class: "
                                + definition.getFullClassName(),
                        exception);
            }

            auditHandler.audit(entity, definition, context);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> CoredeuxEntityAuditHandler<T> resolveAuditHandler(String auditHandlerName, T entity,
            CoredeuxEntityDefinition definition) {
        CoredeuxEntityAuditHandler<?> auditHandler = componentRegistry.getComponent(auditHandlerName,
                CoredeuxEntityAuditHandler.class);
        validateSupportedType(auditHandlerName, auditHandler.getClass(), entity, definition);
        return (CoredeuxEntityAuditHandler<T>) auditHandler;
    }

    protected <T> void validateSupportedType(String beanName, Class<?> beanType, T entity,
            CoredeuxEntityDefinition definition) {
        Class<?> supportedType = CoredeuxGenericTypeResolver.resolveFirstGeneric(beanType,
                CoredeuxEntityAuditHandler.class);
        if (supportedType == null || entity == null || supportedType.isAssignableFrom(entity.getClass())) {
            return;
        }
        throw new CoredeuxStrategyException("Configured audit handler bean '" + beanName
                + "' does not support entity type " + entity.getClass().getName()
                + " for class: " + definition.getFullClassName());
    }

    protected List<String> resolveOperations(CoredeuxModuleDefinition moduleDefinition, CoredeuxEntityDefinition definition) {
        Map<String, Object> configMap = moduleDefinition.getConfigMap();
        Object configured = configMap.get("operations");
        if (!(configured instanceof List<?> rawOperations) || rawOperations.isEmpty()) {
            throw new CoredeuxValidationException(
                    "Audit module requires config.operations for class: " + definition.getFullClassName());
        }

        List<String> operations = rawOperations.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .toList();

        if (operations.isEmpty()) {
            throw new CoredeuxValidationException(
                    "Audit module requires config.operations for class: " + definition.getFullClassName());
        }

        for (String operation : operations) {
            if (!SUPPORTED_OPERATIONS.contains(operation)) {
                throw new CoredeuxValidationException("Unsupported audit operation '" + operation + "' for class: "
                        + definition.getFullClassName());
            }
        }

        return operations;
    }

    protected String mapPhaseToAuditOperation(String phase) {
        return switch (phase) {
            case CoredeuxHookPhases.AFTER_SAVE -> SAVE;
            case CoredeuxHookPhases.AFTER_UPDATE -> UPDATE;
            case CoredeuxHookPhases.BEFORE_DELETE -> DELETE;
            default -> null;
        };
    }
}
