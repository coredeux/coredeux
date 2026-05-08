package com.coredeux.demo.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;
import com.coredeux.demo.domain.Item;

class DemoAuditHandlerTest {

    @Test
    void shouldCaptureAuditEntryAndAllowReset() {
        DemoAuditHandler handler = new DemoAuditHandler();
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .name("customer")
                .fullClassName(TestItem.class.getName())
                .build();
        OperationContext context = OperationContext.builder()
                .lifecycleContext(EntityLifecycleContext.builder()
                        .operation(CoredeuxLifecycleOperations.UPSERT)
                        .identifier(99L)
                        .build())
                .build();

        handler.audit(new TestItem(), definition, context);

        assertEquals(1, handler.getAuditEntries().size());
        DemoAuditHandler.AuditEntry entry = handler.getAuditEntries().get(0);
        assertEquals("customer", entry.entityName());
        assertEquals(CoredeuxLifecycleOperations.UPSERT, entry.operation());
        assertEquals(99L, entry.identifier());

        handler.clear();

        assertTrue(handler.getAuditEntries().isEmpty());
    }

    private static final class TestItem extends Item {
    }
}
