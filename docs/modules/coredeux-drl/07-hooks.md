# Hooks

<!-- docs-nav-start -->
[Previous: Validators](/coredeux-drl-validators) | [Documentation Home](/) | [Next: Audit](/coredeux-drl-audit)
<!-- docs-nav-end -->

This page shows the Java-first way to author DRL-backed lifecycle hooks.

Hooks are one of the places where the full Java source really matters. The
developer should implement every hook method in the interface, because each
method maps to a lifecycle phase that the runtime may invoke separately.

## Java Source First

The runtime contract is `DRLCoredeuxEntityHook<T>`. The example below shows the
entire class, not a shortened fragment.

```java
package com.example.customer.hooks;

import com.coredeux.drl.core.hooks.DRLCoredeuxEntityHook;
import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlGlobal;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;
import com.example.customer.CustomerAuditTrailService;
import com.example.customer.CustomerNotificationService;

@DrlDefinition("customer-hooks")
public class CustomerHookRules implements DRLCoredeuxEntityHook<Customer> {

    @DrlGlobal
    public com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

    private final CustomerAuditTrailService auditTrailService;
    private final CustomerNotificationService notificationService;

    public CustomerHookRules(CustomerAuditTrailService auditTrailService,
            CustomerNotificationService notificationService) {
        this.auditTrailService = auditTrailService;
        this.notificationService = notificationService;
    }

    @Override
    @DrlRule(name = "onLoad", when = "$context : RuleContext(method == 'onLoad')")
    public void onLoad(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        auditTrailService.record("load", customer.getId());
        $context.setMessage("Customer loaded hook completed.");
    }

    @Override
    @DrlRule(name = "beforeSave", when = "$context : RuleContext(method == 'beforeSave')")
    public void beforeSave(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        auditTrailService.record("beforeSave", customer.getId());
        $context.setMessage("Customer before-save hook completed.");
    }

    @Override
    @DrlRule(name = "afterSave", when = "$context : RuleContext(method == 'afterSave')")
    public void afterSave(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        notificationService.customerSaved(customer.getId());
        $context.setMessage("Customer after-save hook completed.");
    }

    @Override
    @DrlRule(name = "beforeUpdate", when = "$context : RuleContext(method == 'beforeUpdate')")
    public void beforeUpdate(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        auditTrailService.record("beforeUpdate", customer.getId());
        $context.setMessage("Customer before-update hook completed.");
    }

    @Override
    @DrlRule(name = "afterUpdate", when = "$context : RuleContext(method == 'afterUpdate')")
    public void afterUpdate(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        notificationService.customerUpdated(customer.getId());
        $context.setMessage("Customer after-update hook completed.");
    }

    @Override
    @DrlRule(name = "beforeDelete", when = "$context : RuleContext(method == 'beforeDelete')")
    public void beforeDelete(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        auditTrailService.record("beforeDelete", customer.getId());
        $context.setMessage("Customer before-delete hook completed.");
    }

    @Override
    @DrlRule(name = "beforeRefresh", when = "$context : RuleContext(method == 'beforeRefresh')")
    public void beforeRefresh(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        auditTrailService.record("beforeRefresh", customer.getId());
        $context.setMessage("Customer before-refresh hook completed.");
    }

    @Override
    @DrlRule(name = "afterRefresh", when = "$context : RuleContext(method == 'afterRefresh')")
    public void afterRefresh(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        notificationService.customerRefreshed(customer.getId());
        $context.setMessage("Customer after-refresh hook completed.");
    }
}
```

That is the shape a developer should expect to maintain in an IDE.

Keep each hook method self-contained for the same reason: the conversion step
does not preserve private helper methods as reusable DRL methods. If a hook
needs shared logic, put it in a separate DRL source or in an external service
that the rule resolves through the registry.

## Generated DRL

The generated DRL keeps the same method names as rule names:

```drl
package com.example.customer.hooks;

import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;
import com.example.customer.CustomerAuditTrailService;
import com.example.customer.CustomerNotificationService;

global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

rule "onLoad"
when
    $context : RuleContext(method == "onLoad")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    CustomerAuditTrailService auditTrailService =
            componentRegistry.getComponent("customerAuditTrailService", CustomerAuditTrailService.class);
    auditTrailService.record("load", customer.getId());
    $context.setMessage("Customer loaded hook completed.");
end

rule "beforeSave"
when
    $context : RuleContext(method == "beforeSave")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    CustomerAuditTrailService auditTrailService =
            componentRegistry.getComponent("customerAuditTrailService", CustomerAuditTrailService.class);
    auditTrailService.record("beforeSave", customer.getId());
    $context.setMessage("Customer before-save hook completed.");
end

rule "afterSave"
when
    $context : RuleContext(method == "afterSave")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    CustomerNotificationService notificationService =
            componentRegistry.getComponent("customerNotificationService", CustomerNotificationService.class);
    notificationService.customerSaved(customer.getId());
    $context.setMessage("Customer after-save hook completed.");
end

rule "beforeUpdate"
when
    $context : RuleContext(method == "beforeUpdate")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    CustomerAuditTrailService auditTrailService =
            componentRegistry.getComponent("customerAuditTrailService", CustomerAuditTrailService.class);
    auditTrailService.record("beforeUpdate", customer.getId());
    $context.setMessage("Customer before-update hook completed.");
end

rule "afterUpdate"
when
    $context : RuleContext(method == "afterUpdate")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    CustomerNotificationService notificationService =
            componentRegistry.getComponent("customerNotificationService", CustomerNotificationService.class);
    notificationService.customerUpdated(customer.getId());
    $context.setMessage("Customer after-update hook completed.");
end

rule "beforeDelete"
when
    $context : RuleContext(method == "beforeDelete")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    CustomerAuditTrailService auditTrailService =
            componentRegistry.getComponent("customerAuditTrailService", CustomerAuditTrailService.class);
    auditTrailService.record("beforeDelete", customer.getId());
    $context.setMessage("Customer before-delete hook completed.");
end

rule "beforeRefresh"
when
    $context : RuleContext(method == "beforeRefresh")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    CustomerAuditTrailService auditTrailService =
            componentRegistry.getComponent("customerAuditTrailService", CustomerAuditTrailService.class);
    auditTrailService.record("beforeRefresh", customer.getId());
    $context.setMessage("Customer before-refresh hook completed.");
end

rule "afterRefresh"
when
    $context : RuleContext(method == "afterRefresh")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    CustomerNotificationService notificationService =
            componentRegistry.getComponent("customerNotificationService", CustomerNotificationService.class);
    notificationService.customerRefreshed(customer.getId());
    $context.setMessage("Customer after-refresh hook completed.");
end
```

## Why Hooks Should Stay Complete

Because the interface has one method per lifecycle phase, the source should not
be collapsed into one generic helper. Showing all methods helps the author see
exactly which phase is being converted and which business action happens in
each phase.

That also makes it clearer to agents:

- the method name is the phase contract
- every phase has its own rule
- `RuleContext.message` is a good place for trace text
- the rule can call approved components through the registry

## What The Rule Receives

The hook context usually contains:

- `params.entity`
- `params.definition`
- `params.context`
- `params.phase`
- `facts` containing the entity

## Good Hook Shape

- keep one hook method per phase
- keep the side effect small and explicit
- use the message field for traceability
- keep the same method names in Java and DRL

## Example Call Site

```java
RuleContext<Customer> context = RuleContext.method("afterSave")
        .param("entity", customer)
        .param("phase", "AFTER_SAVE")
        .fact(customer);

drlService.execute("customer-hooks.drl", context);
```

## Next Step

If you want to emit audit history in the same Java-first style, continue to
the audit page.

<!-- docs-nav-start -->
[Previous: Validators](/coredeux-drl-validators) | [Documentation Home](/) | [Next: Audit](/coredeux-drl-audit)
<!-- docs-nav-end -->
