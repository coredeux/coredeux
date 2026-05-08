package com.coredeux.core.validation;

import java.util.List;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;

/**
 * Extension point for entity validators resolved from configuration.
 */
public interface CoredeuxEntityValidator<T> {

    /**
     * Validates the given entity against the configured entity definition.
     *
     * @param entity the entity being validated
     * @param definition the entity definition
     * @param context the current operation context
     * @return the validation errors produced by the validator, or an empty list
     */
    List<ValidationError> validate(T entity, CoredeuxEntityDefinition definition, OperationContext context);
}
