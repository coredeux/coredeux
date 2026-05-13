package com.coredeux.demo.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.demo.domain.Customer;

public class DemoCustomerApprovalWorkflow implements CoredeuxDemoWorkflowHandler<Customer> {

    private static final Logger LOG = LoggerFactory.getLogger(DemoCustomerApprovalWorkflow.class);

    private final List<WorkflowEvent> events = new ArrayList<>();

    @Override
    public synchronized void execute(Customer customer, CoredeuxEntityDefinition definition, OperationContext context) {
        EntityLifecycleContext<?> lifecycleContext = context.getLifecycleContext();
        WorkflowEvent event = new WorkflowEvent(
                definition.getName(),
                lifecycleContext != null ? lifecycleContext.getOperation() : null,
                lifecycleContext != null ? lifecycleContext.getIdentifier() : null);
        events.add(event);
        LOG.info("Workflow entity {} operation {} identifier {}", event.entityName(), event.operation(),
                event.identifier());
    }

    public synchronized List<WorkflowEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public synchronized void clear() {
        events.clear();
    }

    public record WorkflowEvent(String entityName, String operation, Object identifier) {
    }
}

