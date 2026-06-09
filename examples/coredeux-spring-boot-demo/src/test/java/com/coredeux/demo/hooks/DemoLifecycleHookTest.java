package com.coredeux.demo.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.CustomerOrder;
import com.coredeux.demo.domain.OrderStatus;
import com.coredeux.demo.domain.Product;

class DemoLifecycleHookTest {

    private final DemoLifecycleHook hook = new DemoLifecycleHook();

    @Test
    void shouldTouchCustomerAndOrderEntitiesOnLifecycleEvents() {
        CoredeuxEntityDefinition definition = definition();
        OperationContext context = operationContext("UPSERT", "42");

        Customer customer = Customer.builder().name("Alice").build();
        hook.beforeSave(customer, definition, context);
        hook.afterSave(customer, definition, context);
        assertNotNull(customer.getLastLifecycleTouch());

        CustomerOrder order = CustomerOrder.builder()
                .status(OrderStatus.DRAFT)
                .build();
        hook.beforeSave(order, definition, context);
        assertNotNull(order.getCreatedAt());
        hook.afterUpdate(order, definition, context);
        assertNotNull(order.getCreatedAt());
    }

    @Test
    void shouldTouchProductMetadataAndSupportLoadCallbacks() {
        CoredeuxEntityDefinition definition = definition();
        OperationContext context = operationContext("LOAD", "99");

        Product product = Product.builder()
                .name("Widget")
                .metadata(new LinkedHashMap<>())
                .build();

        hook.onLoad(product, definition, context);
        hook.beforeUpdate(product, definition, context);
        hook.afterUpdate(product, definition, context);

        assertEquals("Widget", product.getName());
        assertNotNull(product.getMetadata().get("lastHookTouch"));
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
