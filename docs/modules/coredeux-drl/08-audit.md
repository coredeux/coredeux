# Audit

<!-- docs-nav-start -->
[Previous: Hooks](/coredeux-drl-hooks) | [Documentation Home](/) | [Next: Custom Handlers](/coredeux-drl-custom-handlers)
<!-- docs-nav-end -->

This page shows the Java-first way to author DRL-backed audit handlers.

Audit is another place where the full source should be shown before the DRL
output. The developer writes the normal class, DevTools generates the rule, and
the runtime executes it by phase.

## Java Source First

The runtime contract is `DRLCoredeuxEntityAuditHandler<T>`.

```java
package com.example.customer.audit;

import com.coredeux.core.audit.CoredeuxEntityDefinition;
import com.coredeux.core.context.OperationContext;
import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlGlobal;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.core.audit.DRLCoredeuxEntityAuditHandler;
import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;
import com.example.customer.CustomerAuditService;

@DrlDefinition("customer-audit")
public class CustomerAuditRules implements DRLCoredeuxEntityAuditHandler<Customer> {

    @DrlGlobal
    public com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

    private final CustomerAuditService auditService;

    public CustomerAuditRules(CustomerAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    @DrlRule(name = "audit", when = "$context : RuleContext(method == 'audit')")
    public void audit(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        String phase = String.valueOf($context.getParams().get("phase"));
        CoredeuxEntityDefinition definition =
                (CoredeuxEntityDefinition) $context.getParams().get("definition");
        OperationContext operationContext =
                (OperationContext) $context.getParams().get("context");

        auditService.record(customer, definition, operationContext, phase);
        $context.setMessage("Audit recorded for phase " + phase + ".");
    }
}
```

The source stays readable because the developer can still navigate imports,
types, and constructor injection in the IDE before any conversion happens.

Just like the other DRL authoring styles, the converted rule should be treated
as self-contained. Shared audit behavior should be moved to another DRL source
or an external service rather than relying on private helper methods inside the
authoring class.

## Generated DRL

```drl
package com.example.customer.audit;

import com.coredeux.core.audit.CoredeuxEntityDefinition;
import com.coredeux.core.context.OperationContext;
import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;
import com.example.customer.CustomerAuditService;

global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

rule "audit"
when
    $context : RuleContext(method == "audit")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    String phase = String.valueOf($context.getParams().get("phase"));
    CoredeuxEntityDefinition definition =
            (CoredeuxEntityDefinition) $context.getParams().get("definition");
    OperationContext operationContext =
            (OperationContext) $context.getParams().get("context");

    CustomerAuditService auditService =
            componentRegistry.getComponent("customerAuditService", CustomerAuditService.class);
    auditService.record(customer, definition, operationContext, phase);
    $context.setMessage("Audit recorded for phase " + phase + ".");
end
```

## What The Audit Context Carries

The audit context usually contains:

- `params.entity`
- `params.definition`
- `params.context`
- `params.phase`
- `facts` containing the entity

## Good Audit Shape

- write one clear audit event per operation
- keep the payload structured
- use `message` for a readable summary
- keep the audit rule easy to trace later

## Example Call Site

```java
RuleContext<Customer> context = RuleContext.method("audit")
        .param("entity", customer)
        .param("definition", definition)
        .param("context", operationContext)
        .param("phase", "AFTER_SAVE")
        .fact(customer);

drlService.execute("customer-audit.drl", context);
```

## When To Use DRL For Audit

Use DRL audit handlers when:

- the audit policy changes often
- the record content depends on business rules
- you want a single audit source to work in native and Spring hosts
- you need the audit action to be editable without rebuilding the app

## Next Step

If you want to see how to build your own module families with the same
Java-first style, continue to the custom handlers page.

<!-- docs-nav-start -->
[Previous: Hooks](/coredeux-drl-hooks) | [Documentation Home](/) | [Next: Custom Handlers](/coredeux-drl-custom-handlers)
<!-- docs-nav-end -->
