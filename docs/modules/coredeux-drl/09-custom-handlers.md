# Custom Handlers

<!-- docs-nav-start -->
[Previous: Audit](/coredeux-drl-audit) | [Documentation Home](/) | [Next: Reference](/coredeux-drl-reference)
<!-- docs-nav-end -->

This page shows how to build your own handler family with a Java-first source
file and an equivalent DRL source.

The built-in DRL support already proves the pattern:

- write the source in Java-like code first
- implement the full contract in the IDE
- use DevTools to generate the DRL text
- store the DRL externally
- resolve and execute the rule by id at runtime

You can reuse that exact shape for custom module families such as pricing,
notifications, compliance, enrichment, or workflow-specific side effects.

## Annotated Source Variant

If the custom handler logic itself is meant to be authored as DRL source, the
Java file should carry the converter annotations too:

```java
package com.example.customer.pricing;

import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlGlobal;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;
import com.example.customer.PricingService;
import com.example.customer.Quote;

@DrlDefinition("customer-pricing-rules")
public class CustomerPricingRules {

    @DrlGlobal
    public com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

    @DrlRule(name = "afterSavePricing", when = "$context : RuleContext(method == 'AFTER_SAVE')")
    public void afterSavePricing(RuleContext<Customer> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        PricingService pricingService = componentRegistry.getComponent("pricingService", PricingService.class);
        Quote quote = pricingService.reprice(customer);
        $context.setOutput(quote);
        $context.setMessage("Pricing recalculated after save.");
    }
}
```

## Java Source First

Suppose we want a `pricing` module that adjusts a customer quote after save.
The Java source would implement the standard module handler contract:

```java
package com.example.customer.pricing;

import java.util.List;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.drl.core.module.DRLCoredeuxEntityModuleHandler;
import com.coredeux.drl.core.service.DRLService;
import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;
import com.example.customer.PricingService;

public class CustomerPricingModuleHandler implements DRLCoredeuxEntityModuleHandler {

    private final DRLService drlService;
    private final PricingService pricingService;

    public CustomerPricingModuleHandler(DRLService drlService, PricingService pricingService) {
        this.drlService = drlService;
        this.pricingService = pricingService;
    }

    @Override
    public String getModuleName() {
        return "pricing";
    }

    @Override
    public <T> void execute(T entity, CoredeuxEntityDefinition definition,
            CoredeuxModuleDefinition moduleDefinition, String phase, OperationContext context) {
        for (String handlerName : moduleDefinition.getHandlers()) {
            if (handlerName == null || handlerName.isBlank()) {
                continue;
            }

            String trimmedName = handlerName.trim();
            if (trimmedName.endsWith(".drl")) {
                RuleContext<Customer> ruleContext = RuleContext.<Customer>method(phase)
                        .param("entity", entity)
                        .param("definition", definition)
                        .param("moduleDefinition", moduleDefinition)
                        .param("phase", phase)
                        .param("context", context)
                        .fact(entity)
                        .fact(pricingService);
                drlService.execute(trimmedName, ruleContext);
                continue;
            }

            // Java handler branch would resolve the bean and call it directly.
            // The exact resolution strategy depends on the module family.
        }
    }
}
```

The important part is the choice point:

- `.drl` means "load the source, compile it, cache it, execute it"
- anything else means "resolve the Java bean and call it directly"

That lets the same module family evolve gradually over time.

When the handler is authored as DRL, keep the converted rule body self-contained
for the same reason as the built-in guides: helper methods in the Java source
are only authoring conveniences and do not survive as reusable DRL methods.
If shared logic is needed, put it in another DRL source or an external
component resolved through the registry.

## Generated DRL

The DRL-backed version of the same module family still looks like a normal rule
source:

```drl
package com.example.customer.pricing;

import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;
import com.example.customer.PricingService;
import com.example.customer.Quote;

global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

rule "afterSavePricing"
when
    $context : RuleContext(method == "AFTER_SAVE")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    PricingService pricingService =
            componentRegistry.getComponent("pricingService", PricingService.class);
    Quote quote = pricingService.reprice(customer);
    $context.setOutput(quote);
    $context.setMessage("Pricing recalculated after save.");
end
```

## Why The Java Source Still Matters

The Java source is the authoring contract. It gives the developer:

- full IDE autocomplete
- explicit imports
- all method signatures visible at once
- a class structure that can be reviewed before conversion

That is the key reason not to compress the source into a tiny fragment.

## How To Think About Custom Handlers

Use the same mental model as the built-in modules:

1. the module name defines the family
2. the handler list defines the order
3. the suffix decides whether the handler is Java or DRL
4. the `RuleContext` carries inputs, facts, output, and messages
5. the runtime executes the selected handler without changing the caller

## Example Module Configuration

```yaml
coredeux:
  entities:
    - full-class-name: com.example.customer.Customer
      entity-name: customer
      identifier:
        field: id
      storage:
        type: memory
        data-access-service: customer-data-access.drl
      modules:
        - name: pricing
          enabled: true
          handlers:
            - customer-pricing-java
            - customer-pricing-rules.drl
```

## Practical Rule

If you can explain the module as:

- "write the Java contract first"
- "convert the DRL-backed handlers with DevTools"
- "store the generated DRL externally"
- "let the runtime choose the correct branch by suffix"

then you are using the intended pattern.

## Next Step

If you need the shorter summary of contracts and update rules, continue to the
reference page.

<!-- docs-nav-start -->
[Previous: Audit](/coredeux-drl-audit) | [Documentation Home](/) | [Next: Reference](/coredeux-drl-reference)
<!-- docs-nav-end -->
