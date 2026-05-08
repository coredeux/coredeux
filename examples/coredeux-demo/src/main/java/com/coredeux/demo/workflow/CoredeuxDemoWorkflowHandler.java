package com.coredeux.demo.workflow;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;

/**
 * Demo-local workflow contract used to show how applications can contribute a
 * custom Coredeux module without changing the core framework.
 */
public interface CoredeuxDemoWorkflowHandler<T> {

    void execute(T entity, CoredeuxEntityDefinition definition, OperationContext context);
}

