package com.coredeux.demo.hooks;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.CustomerOrder;
import com.coredeux.demo.domain.Item;
import com.coredeux.demo.domain.Product;

@Component("demoLifecycleHook")
public class DemoLifecycleHook implements CoredeuxEntityHook<Item> {

    private static final Logger LOG = LoggerFactory.getLogger(DemoLifecycleHook.class);

    @Override
    public void onLoad(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        LOG.info("Loaded entity {} with operation {}", definition.getName(),
                context.getLifecycleContext() != null ? context.getLifecycleContext().getOperation() : "unknown");
    }

    @Override
    public void beforeSave(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
        LOG.info("Before save for {}", definition.getName());
    }

    @Override
    public void afterSave(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
        LOG.info("After save for {}", definition.getName());
    }

    @Override
    public void beforeUpdate(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
        LOG.info("Before update for {}", definition.getName());
    }

    @Override
    public void afterUpdate(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
        LOG.info("After update for {}", definition.getName());
    }

    private void touch(Item entity) {
        if (entity instanceof Customer customer) {
            customer.setLastLifecycleTouch(Instant.now());
        }
        if (entity instanceof CustomerOrder order && order.getCreatedAt() == null) {
            order.setCreatedAt(Instant.now());
        }
        if (entity instanceof Product product && product.getMetadata() != null) {
            product.getMetadata().put("lastHookTouch", Instant.now().toString());
        }
    }
}
