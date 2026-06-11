package com.coredeux.drl.core.validation;

import java.util.List;

import com.coredeux.core.validation.ValidationError;
import com.coredeux.drl.model.RuleContext;

/**
 * Extension point for DRL-backed entity validators resolved from configuration.
 *
 * <p>Implementations should inspect the supplied rule context, perform any
 * validation logic, and place the resulting {@code List<ValidationError>} into
 * the context output if validation errors are found.
 */
public interface DRLCoredeuxEntityValidator {

    /**
     * Validates the current entity against the configured entity definition.
     *
     * @param $context the rule execution context carrying the entity, definition,
     *                 operation context, and the mutable validation result output
     */
    void validate(RuleContext<List<ValidationError>> $context);
}
