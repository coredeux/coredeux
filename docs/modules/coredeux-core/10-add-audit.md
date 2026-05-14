# Add Audit

<!-- docs-nav-start -->
[Previous: Add a Validator](09-add-validator.md) | [Documentation Home](../../README.md) | [Next: Adding a New Entity](11-add-new-entity.md)
<!-- docs-nav-end -->

Audit handlers record meaningful business operations as the framework runs.

They are useful when you want to keep a trace of:

- what entity changed
- which operation happened
- which identifier was involved
- when the change happened

## The Audit Contract

Audit handlers implement `CoredeuxEntityAuditHandler<T>`.

The handler receives:

- the entity
- the entity definition
- the operation context

That gives the audit layer enough information to build a durable audit entry or
an in-memory trace.

## Demo Example: `DemoAuditHandler`

The Spring Boot demo includes `demoAuditHandler`.

It stores audit events in memory and logs them for visibility.

Each audit event captures:

- the logical entity name
- the lifecycle operation
- the identifier

That makes it easy to assert in tests and easy to see in the demo logs.

The same handler is attached to several demo entities:

- `Customer`
- `Product`
- `CustomerOrder`
- `Role`
- `OrderItem`
- `ExportStorageRecord`

## Real Demo Code

The demo audit handler keeps the example intentionally small:

```java
@Component("demoAuditHandler")
public class DemoAuditHandler implements CoredeuxEntityAuditHandler<Item> {

    private final List<AuditEntry> auditEntries = new ArrayList<>();

    @Override
    public synchronized void audit(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        EntityLifecycleContext<?> lifecycleContext = context.getLifecycleContext();
        AuditEntry entry = new AuditEntry(
                definition.getName(),
                lifecycleContext != null ? lifecycleContext.getOperation() : null,
                lifecycleContext != null ? lifecycleContext.getIdentifier() : null);
        auditEntries.add(entry);
        LOG.info("Audit entity {} operation {} identifier {}", entry.entityName(), entry.operation(),
                entry.identifier());
    }
}
```

The demo wires that handler on several entities in YAML:

```yaml
coredeux:
  entities:
    - full-class-name: com.coredeux.demo.domain.Customer
      modules:
        - name: audit
          enabled: true
          handlers:
            - demoAuditHandler
          config:
            operations:
              - SAVE
              - UPDATE
              - DELETE
    - full-class-name: com.coredeux.demo.domain.Product
      modules:
        - name: audit
          enabled: true
          handlers:
            - demoAuditHandler
          config:
            operations:
              - SAVE
              - UPDATE
              - DELETE
```

That shows the core idea of audit in Coredeux: the framework reports the
operation, the entity name, and the identifier, while the handler decides how
to store or log the event.

## YAML Configuration

Audit is enabled per entity through the `audit` module.

```yaml
modules:
  - name: audit
    enabled: true
    handlers:
      - demoAuditHandler
    config:
      operations:
        - SAVE
        - UPDATE
        - DELETE
```

## Supported Operations

The audit module understands these configured operations:

- `SAVE`
- `UPDATE`
- `DELETE`
- `ALL`

The module maps those operations to the right framework phases.

That makes it possible to express business-level audit policy in YAML without
hardcoding lifecycle branches in the application.

## When To Use Audit

Use audit when the framework should remember that something happened, not just
react to it.

Typical audit targets:

- persistence events
- business operations
- export storage lifecycle changes

If the behavior is about state adjustment, use a hook. If it is about rejecting
bad state, use a validator.

## Native Versus Spring

The audit contract is the same in both native and Spring Boot applications.

The only difference is how the bean is registered:

- Spring Boot: declare it as a bean or component
- native Java: register it in your bootstrap runtime or component registry

## What To Read Next

- [Adding a New Entity](11-add-new-entity.md)
- [Adding a Core Module](12-add-core-module.md)

<!-- docs-nav-start -->
[Previous: Add a Validator](09-add-validator.md) | [Documentation Home](../../README.md) | [Next: Adding a New Entity](11-add-new-entity.md)
<!-- docs-nav-end -->
