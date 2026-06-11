package com.coredeux.drl.core.audit;

import com.coredeux.drl.model.RuleContext;

/**
 * Extension point for DRL-backed entity audit handlers resolved from
 * configuration.
 *
 * <p>The supplied rule context carries the entity, operation metadata, and any
 * mutable output the rule may want to publish.
 */
public interface DRLCoredeuxEntityAuditHandler<T> {

    /**
     * Executes the audit behavior for the current entity operation.
     *
     * @param $context the rule execution context for the audit phase
     */
    void audit(RuleContext<T> $context);
}
