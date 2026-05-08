package com.coredeux.core.audit;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;

/**
 * Extension point for entity audit handlers resolved from configuration.
 */
public interface CoredeuxEntityAuditHandler<T> {

    void audit(T entity, CoredeuxEntityDefinition definition, OperationContext context);
}
