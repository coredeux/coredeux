package com.coredeux.demo.audit;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.coredeux.core.audit.CoredeuxEntityAuditHandler;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.demo.domain.Customer;

@Component("demoAuditHandler")
public class DemoAuditHandler implements CoredeuxEntityAuditHandler<Customer> {

    private final List<String> auditEntries = new ArrayList<>();

    @Override
    public synchronized void audit(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
    	System.out.println("Auditing entity: " + entity + " with definition: " + definition.getName() + " in context: "
				+ (context != null ? context.getLifecycleContext() : "null"));
        auditEntries.add(definition.getName() + ":" + (context != null && context.getLifecycleContext() != null
                ? context.getLifecycleContext().getOperation()
                : "unknown"));
    }
}
