package com.coredeux.demo.hooks;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.CustomerStatus;

class DemoLifecycleHookTest {

    @Test
    void logsLifecycleEventsAndTouchesTheCustomer() {
        DemoLifecycleHook hook = new DemoLifecycleHook();
        Customer customer = new Customer();
        customer.setName("Alice");
        customer.setEmail("alice@example.com");
        customer.setActive(true);
        customer.setStatus(CustomerStatus.ACTIVE);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName(Customer.class.getName())
                .name("customer")
                .identifier("pk")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("customerDataAccess.drl").build())
                .build();
        OperationContext context = OperationContext.builder().build();

        hook.onLoad(customer, definition, context);
        hook.afterSave(customer, definition, context);
        hook.afterUpdate(customer, definition, context);
        hook.beforeDelete(customer, definition, context);
        hook.beforeRefresh(customer, definition, context);
        hook.afterRefresh(customer, definition, context);

        assertNull(customer.getLastLifecycleTouch());
    }
}
