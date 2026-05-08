package com.coredeux.core.module.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.coredeux.core.audit.CoredeuxEntityAuditHandler;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.strategy.CoredeuxHookPhases;

/**
 * Executes configured audit handlers for the configured write operations.
 */
@Component
public class AuditModuleHandler implements CoredeuxEntityModuleHandler {

    private static final String MODULE_NAME = "audit";
    private static final String ALL = "ALL";
    private static final String SAVE = "SAVE";
    private static final String UPDATE = "UPDATE";
    private static final String DELETE = "DELETE";
    private static final List<String> SUPPORTED_OPERATIONS = List.of(ALL, SAVE, UPDATE, DELETE);

    private final ApplicationContext applicationContext;

    public AuditModuleHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
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
    private <T> CoredeuxEntityAuditHandler<T> resolveAuditHandler(String auditHandlerName, T entity,
            CoredeuxEntityDefinition definition) {
        CoredeuxEntityAuditHandler<?> auditHandler = applicationContext.getBean(auditHandlerName,
                CoredeuxEntityAuditHandler.class);
        validateSupportedType(auditHandlerName, auditHandler.getClass(), entity, definition);
        return (CoredeuxEntityAuditHandler<T>) auditHandler;
    }

    private <T> void validateSupportedType(String beanName, Class<?> beanType, T entity,
            CoredeuxEntityDefinition definition) {
        Class<?> supportedType = ResolvableType.forClass(beanType)
                .as(CoredeuxEntityAuditHandler.class)
                .resolveGeneric(0);
        if (supportedType == null || entity == null || supportedType.isAssignableFrom(entity.getClass())) {
            return;
        }
        throw new CoredeuxStrategyException("Configured audit handler bean '" + beanName
                + "' does not support entity type " + entity.getClass().getName()
                + " for class: " + definition.getFullClassName());
    }

    private List<String> resolveOperations(CoredeuxModuleDefinition moduleDefinition, CoredeuxEntityDefinition definition) {
        Map<String, Object> configMap = moduleDefinition.getConfigMap();
        Object configured = configMap.get("operations");
        if (!(configured instanceof List<?> rawOperations) || CollectionUtils.isEmpty(rawOperations)) {
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

    private String mapPhaseToAuditOperation(String phase) {
        return switch (phase) {
            case CoredeuxHookPhases.AFTER_SAVE -> SAVE;
            case CoredeuxHookPhases.AFTER_UPDATE -> UPDATE;
            case CoredeuxHookPhases.BEFORE_DELETE -> DELETE;
            default -> null;
        };
    }
}
