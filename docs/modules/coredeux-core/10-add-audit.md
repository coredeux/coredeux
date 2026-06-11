# Add Audit

<!-- docs-nav-start -->
[Previous: Add a Validator](/modules/coredeux-core/09-add-validator) | [Documentation Home](/) | [Next: Adding a New Entity](/modules/coredeux-core/11-add-new-entity)
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

The module maps those operations to specific framework phases:

| Audit operation | Framework phase | When the handler runs |
| --- | --- | --- |
| `SAVE` | `AFTER_SAVE` | After `CoredeuxDataAccessService.save(...)` has completed |
| `UPDATE` | `AFTER_UPDATE` | After `CoredeuxDataAccessService.update(...)` has completed |
| `DELETE` | `BEFORE_DELETE` | Before `CoredeuxDataAccessService.remove(...)` is called |
| `ALL` | `AFTER_SAVE`, `AFTER_UPDATE`, `BEFORE_DELETE` | For every audit-supported write operation |

The important detail is that audit is not called for every lifecycle phase.
It is only called when the phase maps to one of the configured audit
operations.

For example, if an entity has this configuration:

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
```

then `demoAuditHandler` runs after saves and updates, but not before deletes.
If the same entity configures `DELETE`, the handler runs before the delete is
sent to the data access service.

This is the mapping used internally by the audit module:

```java
private String mapPhaseToAuditOperation(String phase) {
    return switch (phase) {
        case CoredeuxHookPhases.AFTER_SAVE -> SAVE;
        case CoredeuxHookPhases.AFTER_UPDATE -> UPDATE;
        case CoredeuxHookPhases.BEFORE_DELETE -> DELETE;
        default -> null;
    };
}
```

The delete audit runs before removal because the existing entity is still
available at that point. The handler can inspect the object that is about to be
deleted and write a meaningful audit record before the storage adapter removes
it.

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

- [Adding a New Entity](/modules/coredeux-core/11-add-new-entity)
- [Adding a Core Module](/modules/coredeux-core/12-add-core-module)

<!-- docs-nav-start -->
[Previous: Add a Validator](/modules/coredeux-core/09-add-validator) | [Documentation Home](/) | [Next: Adding a New Entity](/modules/coredeux-core/11-add-new-entity)
<!-- docs-nav-end -->
