# Add A Validator

<!-- docs-nav-start -->
[Previous: Add A Hook](08-add-hook.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Add Audit](10-add-audit.md)
<!-- docs-nav-end -->

Validators stop bad entity state before the framework saves or updates it.

They are the right tool when the entity must satisfy business rules such as:

- required fields
- field format checks
- conditional business constraints
- cross-field consistency

## The Validator Contract

Validators implement `CoredeuxEntityValidator<T>`.

The validator returns a list of `ValidationError` objects.

If the list is not empty, Coredeux aggregates the errors and raises a
validation exception.

## Demo Example: `CustomerEmailValidator`

The Spring Boot demo includes a validator named `customerEmailValidator`.

It checks three business conditions on `Customer`:

- `name` must not be blank
- `email` must contain `@`
- `status` must be present

That gives the demo a simple but realistic validation example:

- one validator
- multiple field checks
- multiple validation errors in one pass

## Real Demo Code

The demo validator is short and direct:

```java
@Component("customerEmailValidator")
public class CustomerEmailValidator implements CoredeuxEntityValidator<Customer> {

    @Override
    public List<ValidationError> validate(Customer customer, CoredeuxEntityDefinition definition,
            OperationContext context) {
        List<ValidationError> errors = new ArrayList<>();
        if (customer.getName() == null || customer.getName().isBlank()) {
            errors.add(ValidationError.builder().field("name").message("Customer name is required").build());
        }
        if (customer.getEmail() == null || !customer.getEmail().contains("@")) {
            errors.add(ValidationError.builder().field("email").message("Customer email must be valid").build());
        }
        if (customer.getStatus() == null) {
            errors.add(ValidationError.builder().field("status").message("Customer status is required").build());
        }
        return errors;
    }
}
```

The demo applies it in YAML on the customer entity:

```yaml
coredeux:
  entities:
    - full-class-name: com.coredeux.demo.domain.Customer
      modules:
        - name: validators
          enabled: true
          handlers:
            - customerEmailValidator
```

In practice the validator catches bad customer input during create and update
flows, so the app can reject invalid state before it reaches persistence.

## YAML Configuration

Validators are enabled per entity through the `validators` module.

```yaml
modules:
  - name: validators
    enabled: true
    handlers:
      - customerEmailValidator
```

If you want multiple validators for the same entity, list them all in the
`handlers` array.

## Why Validators Matter

Validators keep business rules close to the entity lifecycle.

That means:

- invalid data fails early
- callers get a structured error list
- save and update flows stay predictable
- the framework does not need the application to reimplement the same checks in
  every controller or service

## How Validators Differ From Hooks

Hooks can modify state or react to lifecycle phases.

Validators do not change the entity. They only say whether the entity is valid.

Use a hook when you want to enrich or react. Use a validator when you want to
approve or reject the entity state.

## Native Versus Spring

The validator contract is the same in both native and Spring Boot applications.

The only difference is how the bean is registered:

- Spring Boot: declare it as a bean or component
- native Java: register it in your bootstrap runtime or component registry

## What To Read Next

- [Add Audit](10-add-audit.md)
- [Adding A New Entity](11-add-new-entity.md)

<!-- docs-nav-start -->
[Previous: Add A Hook](08-add-hook.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Add Audit](10-add-audit.md)
<!-- docs-nav-end -->
