package com.coredeux.examples.nativejava.module;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.examples.nativejava.domain.Customer;

public class CustomerLifecycleHook implements CoredeuxEntityHook<Customer> {

    @Override
    public void afterSave(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        System.out.println("Hook afterSave: " + entity);
    }

    @Override
    public void onLoad(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
        System.out.println("Hook onLoad: " + entity);
    }
}
