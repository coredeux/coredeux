package com.coredeux.demo.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.CustomerStatus;

class DemoAuditHandlerTest {

    @Test
    void storesAuditEntriesInMemory() throws Exception {
        DemoAuditHandler handler = new DemoAuditHandler();
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

        handler.audit(customer, definition, context);

        Field field = DemoAuditHandler.class.getDeclaredField("auditEntries");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> auditEntries = (List<String>) field.get(handler);
        assertEquals(1, auditEntries.size());
        assertEquals("customer:unknown", auditEntries.get(0));
    }
}
