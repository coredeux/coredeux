package com.coredeux.demo.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;
import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.Product;

class WorkflowsModuleHandlerTest {

    @Test
    void shouldExposeModuleName() {
        assertEquals("workflows", new WorkflowsModuleHandler(new StaticApplicationContext()).getModuleName());
    }

    @Test
    void shouldInvokeConfiguredWorkflowForMatchingPhase() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        DemoCustomerApprovalWorkflow workflow = new DemoCustomerApprovalWorkflow();
        applicationContext.getBeanFactory().registerSingleton("customerApprovalWorkflow", workflow);

        WorkflowsModuleHandler handler = new WorkflowsModuleHandler(applicationContext);
        Customer customer = Customer.builder().name("Alice").build();

        handler.execute(customer, customerDefinition(), workflowModule(CoredeuxHookPhases.AFTER_SAVE),
                CoredeuxHookPhases.AFTER_SAVE, operationContext(CoredeuxLifecycleOperations.UPSERT, "42"));

        assertEquals(1, workflow.getEvents().size());
        assertEquals("customer", workflow.getEvents().get(0).entityName());
        assertEquals(CoredeuxLifecycleOperations.UPSERT, workflow.getEvents().get(0).operation());
        assertEquals("42", workflow.getEvents().get(0).identifier());
    }

    @Test
    void shouldSkipWorkflowWhenPhaseIsNotConfigured() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        DemoCustomerApprovalWorkflow workflow = new DemoCustomerApprovalWorkflow();
        applicationContext.getBeanFactory().registerSingleton("customerApprovalWorkflow", workflow);

        WorkflowsModuleHandler handler = new WorkflowsModuleHandler(applicationContext);

        handler.execute(Customer.builder().name("Alice").build(), customerDefinition(),
                workflowModule(CoredeuxHookPhases.AFTER_SAVE), CoredeuxHookPhases.BEFORE_SAVE,
                operationContext(CoredeuxLifecycleOperations.UPSERT, null));

        assertTrue(workflow.getEvents().isEmpty());
    }

    @Test
    void shouldRunWhenPhaseConfigurationIsMissing() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        DemoCustomerApprovalWorkflow workflow = new DemoCustomerApprovalWorkflow();
        applicationContext.getBeanFactory().registerSingleton("customerApprovalWorkflow", workflow);

        WorkflowsModuleHandler handler = new WorkflowsModuleHandler(applicationContext);
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("workflows")
                .enabled(true)
                .handlers(List.of("customerApprovalWorkflow"))
                .config(Map.of())
                .build();

        handler.execute(Customer.builder().name("Alice").build(), customerDefinition(), moduleDefinition,
                CoredeuxHookPhases.AFTER_SAVE, operationContext(CoredeuxLifecycleOperations.UPSERT, "42"));

        assertEquals(1, workflow.getEvents().size());
    }

    @Test
    void shouldSkipBlankHandlerNames() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        DemoCustomerApprovalWorkflow workflow = new DemoCustomerApprovalWorkflow();
        applicationContext.getBeanFactory().registerSingleton("customerApprovalWorkflow", workflow);

        WorkflowsModuleHandler handler = new WorkflowsModuleHandler(applicationContext);
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("workflows")
                .enabled(true)
                .handlers(List.of(" "))
                .config(Map.of("phases", List.of(CoredeuxHookPhases.AFTER_SAVE)))
                .build();

        handler.execute(Customer.builder().name("Alice").build(), customerDefinition(), moduleDefinition,
                CoredeuxHookPhases.AFTER_SAVE, operationContext(CoredeuxLifecycleOperations.UPSERT, "42"));

        assertTrue(workflow.getEvents().isEmpty());
    }

    @Test
    void shouldRejectWorkflowBeanForUnsupportedEntityType() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("customerApprovalWorkflow",
                new DemoCustomerApprovalWorkflow());

        WorkflowsModuleHandler handler = new WorkflowsModuleHandler(applicationContext);

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(Product.builder().name("Widget").build(), productDefinition(),
                        workflowModule(CoredeuxHookPhases.AFTER_SAVE), CoredeuxHookPhases.AFTER_SAVE,
                        operationContext(CoredeuxLifecycleOperations.UPSERT, null)));

        assertTrue(exception.getMessage().contains("does not support entity type"));
    }

    @Test
    void shouldRejectMissingWorkflowBean() {
        WorkflowsModuleHandler handler = new WorkflowsModuleHandler(new StaticApplicationContext());

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(Customer.builder().name("Alice").build(), customerDefinition(),
                        workflowModule(CoredeuxHookPhases.AFTER_SAVE), CoredeuxHookPhases.AFTER_SAVE,
                        operationContext(CoredeuxLifecycleOperations.UPSERT, null)));

        assertTrue(exception.getMessage().contains("Unable to resolve workflow handler bean"));
    }

    @Test
    void shouldRejectInvalidPhasesConfig() {
        WorkflowsModuleHandler handler = new WorkflowsModuleHandler(new StaticApplicationContext());

        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("workflows")
                .enabled(true)
                .handlers(List.of("customerApprovalWorkflow"))
                .config(Map.of("phases", "after-save"))
                .build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(Customer.builder().name("Alice").build(), customerDefinition(),
                        moduleDefinition, CoredeuxHookPhases.AFTER_SAVE,
                        operationContext(CoredeuxLifecycleOperations.UPSERT, null)));

        assertTrue(exception.getMessage().contains("config.phases"));
    }

    private CoredeuxEntityDefinition customerDefinition() {
        return CoredeuxEntityDefinition.builder()
                .name("customer")
                .fullClassName(Customer.class.getName())
                .identifier("pk")
                .build();
    }

    private CoredeuxEntityDefinition productDefinition() {
        return CoredeuxEntityDefinition.builder()
                .name("product")
                .fullClassName(Product.class.getName())
                .identifier("pk")
                .build();
    }

    private CoredeuxModuleDefinition workflowModule(String phase) {
        return CoredeuxModuleDefinition.builder()
                .name("workflows")
                .enabled(true)
                .handlers(List.of("customerApprovalWorkflow"))
                .config(Map.of("phases", List.of(phase)))
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
