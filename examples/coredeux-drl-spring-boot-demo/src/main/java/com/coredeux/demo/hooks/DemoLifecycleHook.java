package com.coredeux.demo.hooks;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.demo.domain.Customer;

@Component("demoLifecycleHook")
public class DemoLifecycleHook implements CoredeuxEntityHook<Customer> {

    @Override
    public void onLoad(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
    }

    @Override
    public void beforeSave(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
    }

    @Override
    public void afterSave(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
    }

    @Override
    public void beforeUpdate(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
    }

    @Override
    public void afterUpdate(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
    }

    @Override
    public void beforeDelete(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
    }

    @Override
    public void beforeRefresh(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
    }

    @Override
    public void afterRefresh(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
    }

    private void touch(Customer customer) {
        if (customer != null) {
            customer.setLastLifecycleTouch(Instant.now());
        }
    }
}
