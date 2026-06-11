# Validators

<!-- docs-nav-start -->
[Previous: Data Access](/modules/coredeux-drl/05-data-access) | [Documentation Home](/) | [Next: Hooks](/modules/coredeux-drl/07-hooks)
<!-- docs-nav-end -->

This page shows the Java-first way to author DRL-backed validators.

The most important idea is that the validator source should still look like
real Java in the IDE. The developer writes the full class first, including the
interface contract, and then DevTools turns that source into DRL.

## Java Source First

The runtime contract is `DRLCoredeuxEntityValidator`. It has a single method,
so the Java source is straightforward, but the class should still be complete
and explicit about imports and types.

```java
package com.example.customer.validation;

import java.util.ArrayList;
import java.util.List;

import com.coredeux.core.validation.ValidationError;
import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.core.validation.DRLCoredeuxEntityValidator;
import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;

@DrlDefinition("customer-validator")
public class CustomerValidatorRules implements DRLCoredeuxEntityValidator {

    @Override
    @DrlRule(name = "validate", when = "$context : RuleContext(method == 'validate')")
    public void validate(RuleContext<List<ValidationError>> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        List<ValidationError> errors = new ArrayList<>();

        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            errors.add(ValidationError.builder()
                    .field("email")
                    .message("Email is required.")
                    .build());
        }

        if (customer.getName() == null || customer.getName().isBlank()) {
            errors.add(ValidationError.builder()
                    .field("name")
                    .message("Name is required.")
                    .build());
        }

        if (customer.getStatus() == null) {
            errors.add(ValidationError.builder()
                    .field("status")
                    .message("Status is required.")
                    .build());
        }

        $context.setOutput(errors);
        $context.setMessage(errors.isEmpty() ? "Validation passed." : "Validation failed.");
    }
}
```

The class is intentionally direct. A new developer can read it without learning
special framework syntax first.

Like the other DRL authoring styles, the converted validator should be treated
as self-contained. If several validation methods need the same logic, move that
logic into another DRL source or an external service instead of expecting a
private helper method in the Java source to survive conversion.

## Generated DRL

The generated rule is the DRL form of the same validator:

```drl
package com.example.customer.validation;

import java.util.ArrayList;
import java.util.List;
import com.coredeux.core.validation.ValidationError;
import com.coredeux.drl.model.RuleContext;
import com.example.customer.Customer;

rule "validate"
when
    $context : RuleContext(method == "validate")
then
    Customer customer = (Customer) $context.getParams().get("entity");
    List<ValidationError> errors = new ArrayList<>();

    if (customer.getEmail() == null || customer.getEmail().isBlank()) {
        errors.add(ValidationError.builder()
                .field("email")
                .message("Email is required.")
                .build());
    }

    if (customer.getName() == null || customer.getName().isBlank()) {
        errors.add(ValidationError.builder()
                .field("name")
                .message("Name is required.")
                .build());
    }

    if (customer.getStatus() == null) {
        errors.add(ValidationError.builder()
                .field("status")
                .message("Status is required.")
                .build());
    }

    $context.setOutput(errors);
    $context.setMessage(errors.isEmpty() ? "Validation passed." : "Validation failed.");
end
```

## Why The Full Source Helps

Keeping the Java source complete is useful because:

- the IDE can resolve imports and types
- every method is visible as a normal contract implementation
- the developer does not have to guess what the DRL will look like later
- DevTools can convert the source without inventing missing context

## What The Validator Receives

The validator context usually contains:

- `params.entity`
- `params.definition`
- `params.context`
- `params.phase`
- `facts` containing the entity

That is enough for most validation rules. If you need more, add it explicitly
to the calling context instead of hiding it in the rule source.

## Good Validator Shape

- validate domain rules close to the entity
- return a list of structured `ValidationError` objects
- use `message` for a readable summary
- keep the code side-effect free
- prefer explicit casts when reading from `params`

## Example Call Site

```java
RuleContext<List<ValidationError>> context = RuleContext.method("validate")
        .param("entity", customer)
        .fact(customer);

drlService.execute("customer-validator.drl", context);

List<ValidationError> errors = context.getOutput();
```

## When To Split Rules

If the validator becomes too large, split it by concern:

- one rule source for basic field checks
- one rule source for state transitions
- one rule source for cross-field business constraints

That keeps the generated DRL readable and easier to debug later.

## Next Step

If you want lifecycle behavior around load/save/update/delete, continue to the
hooks page.

<!-- docs-nav-start -->
[Previous: Data Access](/modules/coredeux-drl/05-data-access) | [Documentation Home](/) | [Next: Hooks](/modules/coredeux-drl/07-hooks)
<!-- docs-nav-end -->
