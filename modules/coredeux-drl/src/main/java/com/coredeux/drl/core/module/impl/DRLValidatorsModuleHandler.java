package com.coredeux.drl.core.module.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.module.impl.ValidatorsModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.validation.CoredeuxEntityValidator;
import com.coredeux.core.validation.ValidationError;
import com.coredeux.drl.exceptions.CoredeuxDRLException;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;
import com.coredeux.drl.support.RuleContextExecutionSupport;

/**
 * Executes configured validators for write operations.
 */
public class DRLValidatorsModuleHandler extends ValidatorsModuleHandler implements CoredeuxEntityModuleHandler {

    private static final String DRL_SUFFIX = ".drl";
    private static final String DRL_SERVICE_BEAN_NAME = "coredeuxDrlService";

    private final CoredeuxComponentRegistry componentRegistry;

    public DRLValidatorsModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        super(componentRegistry);
        this.componentRegistry = componentRegistry;
    }

    @Override
    public String getModuleName() {
        return "validators";
    }

    @Override
    public <T> void execute(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition,
            String phase, OperationContext context) {
        if (!CoredeuxHookPhases.BEFORE_SAVE.equals(phase) && !CoredeuxHookPhases.BEFORE_UPDATE.equals(phase)) {
            return;
        }

        List<ValidationError> validationErrors = new ArrayList<>();
        if (moduleDefinition.getHandlers() != null) {
            for (String validatorName : moduleDefinition.getHandlers()) {
                if (StringUtils.isBlank(validatorName)) {
                    continue;
                }

                String trimmedName = validatorName.trim();
                if (isDrlHandler(trimmedName)) {
                    validationErrors.addAll(executeDrlValidator(trimmedName, entity, definition, context));
                    continue;
                }

                validationErrors.addAll(executeJavaValidator(trimmedName, entity, definition, context));
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new CoredeuxValidationException("Validation failed for class: " + definition.getFullClassName(),
                    validationErrors);
        }
    }

    private <T> List<ValidationError> executeJavaValidator(String validatorName, T entity,
            CoredeuxEntityDefinition definition, OperationContext context) {
        try {
            CoredeuxEntityValidator<T> validator = resolveValidator(validatorName, entity, definition);
            List<ValidationError> errors = validator.validate(entity, definition, context);
            return errors != null ? errors : List.of();
        } catch (CoredeuxStrategyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CoredeuxStrategyException("Unable to resolve validator bean: " + validatorName
                    + " for class: " + definition.getFullClassName(), exception);
        }
    }

    private <T> List<ValidationError> executeDrlValidator(String validatorName, T entity,
            CoredeuxEntityDefinition definition, OperationContext context) {
        RuleContext<List<ValidationError>> ruleContext = RuleContext.<List<ValidationError>>method("validate")
                .param("entity", entity)
                .param("definition", definition)
                .param("context", context)
                .param("phase", context != null && context.getLifecycleContext() != null
                        ? context.getLifecycleContext().getOperation()
                        : null)
                .fact(entity);
        try {
            drlService().execute(validatorName, ruleContext);
            RuleContextExecutionSupport.throwIfException(ruleContext,
                    "DRL validator '" + validatorName + "' for class: " + definition.getFullClassName());
            List<ValidationError> output = ruleContext.getOutput();
            return output != null ? output : List.of();
        } catch (CoredeuxDRLException exception) {
            throw exception;
        } catch (CoredeuxStrategyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CoredeuxStrategyException("Unable to execute DRL validator: " + validatorName
                    + " for class: " + definition.getFullClassName(), exception);
        }
    }

    private boolean isDrlHandler(String handlerName) {
        return StringUtils.isNotBlank(handlerName) && handlerName.endsWith(DRL_SUFFIX);
    }

    private DRLService drlService() {
        return componentRegistry.getComponent(DRL_SERVICE_BEAN_NAME, DRLService.class);
    }

}
