package com.coredeux.demo.hooks;

import org.springframework.stereotype.Component;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.demo.domain.Customer;

@Component("demoLifecycleHook")
public class DemoLifecycleHook implements CoredeuxEntityHook<Customer> {

	@Override
	public void onLoad(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
		System.out.println("On Load hook invoked for customer: " + entity.getName());
	}

	@Override
	public void afterSave(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
		System.out.println("After Save hook invoked for customer: " + entity.getName());
	}

	@Override
	public void afterUpdate(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
		System.out.println("After Update hook invoked for customer: " + entity.getName());
	}

	@Override
	public void beforeDelete(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
		System.out.println("Before Delete hook invoked for customer: " + entity.getName());
	}

	@Override
	public void beforeRefresh(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
		System.out.println("Before Refresh hook invoked for customer: " + entity.getName());
	}

	@Override
	public void afterRefresh(Customer entity, CoredeuxEntityDefinition definition, OperationContext context) {
		System.out.println("After Refresh hook invoked for customer: " + entity.getName());
	}
}
