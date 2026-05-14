package com.coredeux.examples.nativejava.module;

import com.coredeux.core.audit.CoredeuxEntityAuditHandler;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.demo.domain.Customer;

public class CustomerAuditHandler implements CoredeuxEntityAuditHandler<Customer> {

    @Override
    public void audit(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        System.out.println("Audit " + definition.getName() + " operation "
                + context.getLifecycleContext().getOperation() + " identifier "
                + context.getLifecycleContext().getIdentifier());
    }
}
