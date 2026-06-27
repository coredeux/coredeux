package com.coredeux.drl.core.module.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.coredeux.core.audit.CoredeuxEntityAuditHandler;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.module.impl.AuditModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.drl.exceptions.CoredeuxDRLException;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;
import com.coredeux.drl.support.RuleContextExecutionSupport;

/**
 * Executes configured audit handlers for the configured write operations.
 */
public class DRLAuditModuleHandler extends AuditModuleHandler implements CoredeuxEntityModuleHandler {

    private static final String DRL_SUFFIX = ".drl";
    private static final String DRL_SERVICE_BEAN_NAME = "coredeuxDrlService";

    private final CoredeuxComponentRegistry componentRegistry;

    public DRLAuditModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        super(componentRegistry);
        this.componentRegistry = componentRegistry;
    }

    @Override
    public String getModuleName() {
        return super.getModuleName();
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

        if (moduleDefinition.getHandlers() != null) {
            for (String auditHandlerName : moduleDefinition.getHandlers()) {
                if (StringUtils.isBlank(auditHandlerName)) {
                    continue;
                }

                String trimmedName = auditHandlerName.trim();
                if (isDrlHandler(trimmedName)) {
                    executeDrlAudit(trimmedName, entity, definition, context);
                    continue;
                }

                executeJavaAudit(trimmedName, entity, definition, context);
            }
        }
    }

    private <T> void executeDrlAudit(String auditHandlerName, T entity, CoredeuxEntityDefinition definition,
            OperationContext context) {
        RuleContext<Void> ruleContext = RuleContext.<Void>method("audit")
                .param("entity", entity)
                .param("definition", definition)
                .param("context", context)
                .param("phase", context != null && context.getLifecycleContext() != null
                        ? context.getLifecycleContext().getOperation()
                        : null)
                .fact(entity);
        try {
            drlService().execute(auditHandlerName, ruleContext);
            RuleContextExecutionSupport.throwIfException(ruleContext,
                    "DRL audit handler '" + auditHandlerName + "' for class: " + definition.getFullClassName());
        } catch (CoredeuxDRLException exception) {
            throw exception;
        } catch (CoredeuxStrategyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CoredeuxStrategyException(
                    "Unable to execute DRL audit handler: " + auditHandlerName + " for class: "
                            + definition.getFullClassName(),
                    exception);
        }
    }

    private boolean isDrlHandler(String handlerName) {
        return StringUtils.isNotBlank(handlerName) && handlerName.endsWith(DRL_SUFFIX);
    }

    private <T> void executeJavaAudit(String auditHandlerName, T entity, CoredeuxEntityDefinition definition,
            OperationContext context) {
        try {
            CoredeuxEntityAuditHandler<T> auditHandler = resolveAuditHandler(auditHandlerName, entity, definition);
            auditHandler.audit(entity, definition, context);
        } catch (CoredeuxStrategyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CoredeuxStrategyException(
                    "Unable to resolve audit handler bean: " + auditHandlerName + " for class: "
                            + definition.getFullClassName(),
                    exception);
        }
    }

    private DRLService drlService() {
        return componentRegistry.getComponent(DRL_SERVICE_BEAN_NAME, DRLService.class);
    }
}
