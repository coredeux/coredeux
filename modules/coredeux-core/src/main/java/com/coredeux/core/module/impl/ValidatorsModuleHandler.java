package com.coredeux.core.module.impl;

import java.util.ArrayList;
import java.util.List;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.util.CoredeuxGenericTypeResolver;
import com.coredeux.core.validation.CoredeuxEntityValidator;
import com.coredeux.core.validation.ValidationError;

/**
 * Executes configured validators for write operations.
 */
public class ValidatorsModuleHandler implements CoredeuxEntityModuleHandler {

    private final CoredeuxComponentRegistry componentRegistry;

    public ValidatorsModuleHandler(CoredeuxComponentRegistry componentRegistry) {
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
        for (String validatorName : moduleDefinition.getHandlers()) {
            if (validatorName == null || validatorName.isBlank()) {
                continue;
            }

            CoredeuxEntityValidator<T> validator;
            try {
                validator = resolveValidator(validatorName.trim(), entity, definition);
            } catch (CoredeuxStrategyException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new CoredeuxStrategyException("Unable to resolve validator bean: " + validatorName
                        + " for class: " + definition.getFullClassName(), exception);
            }

            List<ValidationError> errors = validator.validate(entity, definition, context);
            if (errors != null && !errors.isEmpty()) {
                validationErrors.addAll(errors);
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new CoredeuxValidationException("Validation failed for class: " + definition.getFullClassName(),
                    validationErrors);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> CoredeuxEntityValidator<T> resolveValidator(String validatorName, T entity,
            CoredeuxEntityDefinition definition) {
        CoredeuxEntityValidator<?> validator = componentRegistry.getComponent(validatorName,
                CoredeuxEntityValidator.class);
        validateSupportedType(validatorName, validator.getClass(), entity, definition, "validator");
        return (CoredeuxEntityValidator<T>) validator;
    }

    protected <T> void validateSupportedType(String beanName, Class<?> beanType, T entity,
            CoredeuxEntityDefinition definition, String contractName) {
        Class<?> supportedType = CoredeuxGenericTypeResolver.resolveFirstGeneric(beanType,
                CoredeuxEntityValidator.class);
        if (supportedType == null || entity == null || supportedType.isAssignableFrom(entity.getClass())) {
            return;
        }
        throw new CoredeuxStrategyException("Configured " + contractName + " bean '" + beanName
                + "' does not support entity type " + entity.getClass().getName()
                + " for class: " + definition.getFullClassName());
    }
}
