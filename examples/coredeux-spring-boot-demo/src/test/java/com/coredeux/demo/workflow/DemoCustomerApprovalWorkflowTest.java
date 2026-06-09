package com.coredeux.demo.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.demo.domain.Customer;

class DemoCustomerApprovalWorkflowTest {

    @Test
    void shouldCaptureWorkflowEventsAndExposeImmutableSnapshot() {
        DemoCustomerApprovalWorkflow workflow = new DemoCustomerApprovalWorkflow();
        workflow.execute(Customer.builder().name("Alice").build(), definition(),
                operationContext("UPSERT", "42"));

        List<DemoCustomerApprovalWorkflow.WorkflowEvent> events = workflow.getEvents();
        assertEquals(1, events.size());
        assertEquals("customer", events.get(0).entityName());
        assertEquals("UPSERT", events.get(0).operation());
        assertEquals("42", String.valueOf(events.get(0).identifier()));

        workflow.clear();
        assertTrue(workflow.getEvents().isEmpty());
    }

    private CoredeuxEntityDefinition definition() {
        return CoredeuxEntityDefinition.builder()
                .name("customer")
                .fullClassName(Customer.class.getName())
                .identifier("pk")
                .build();
    }

    private OperationContext operationContext(String operation, Object identifier) {
        return OperationContext.builder()
                .lifecycleContext(EntityLifecycleContext.builder()
                        .operation(operation)
                        .identifier(identifier)
                        .build())
                .build();
    }
}
