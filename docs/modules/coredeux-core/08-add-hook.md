# Add a Hook

<!-- docs-nav-start -->
[Previous: Available Data Access Implementations](07-available-data-access-implementations.md) | [Documentation Home](../../README.md) | [Next: Add a Validator](09-add-validator.md)
<!-- docs-nav-end -->

Hooks let Coredeux react to lifecycle phases around an entity operation.

They are the right tool when your code needs to:

- update timestamps
- derive additional values
- populate metadata
- clean up side effects
- react to load/save/update/delete/refresh phases

## The Hook Contract

Hooks implement `CoredeuxEntityHook<T>`.

The main methods are:

- `onLoad`
- `beforeSave`
- `afterSave`
- `beforeUpdate`
- `afterUpdate`
- `beforeDelete`
- `beforeRefresh`
- `afterRefresh`

The framework decides which method to call based on the current phase.

## Demo Example: `DemoLifecycleHook`

The Spring Boot demo includes a reusable lifecycle hook named
`demoLifecycleHook`.

It applies to the common item-style entities in the demo:

- `Customer`
- `Product`
- `CustomerOrder`

What it does:

- on load, it logs the entity name and lifecycle operation
- before save and after save, it updates lifecycle-related timestamps
- before update and after update, it updates the same lifecycle markers
- it touches different fields depending on the actual entity type

That makes the hook useful as a real example of a generic hook that still knows
how to make entity-specific changes.

## Demo Example: `ExportStorageCleanupHook`

The demo also includes a hook dedicated to export storage cleanup:

- bean name: `exportStorageCleanupHook`
- entity type: `ExportStorageRecord`

This hook runs before delete and removes filesystem-backed export artifacts when
the export storage row is being deleted.

That example is useful because it shows a second hook style:

- not a lifecycle timestamp hook
- a cleanup hook tied to a specific resource type

## Real Demo Code

The generic lifecycle hook in the demo looks like this:

```java
@Component("demoLifecycleHook")
public class DemoLifecycleHook implements CoredeuxEntityHook<Item> {

    @Override
    public void onLoad(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        LOG.info("Loaded entity {} with operation {}", definition.getName(),
                context.getLifecycleContext() != null ? context.getLifecycleContext().getOperation() : "unknown");
    }

    @Override
    public void beforeSave(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
        LOG.info("Before save for {}", definition.getName());
    }

    @Override
    public void afterSave(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
        LOG.info("After save for {}", definition.getName());
    }

    @Override
    public void beforeUpdate(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
        LOG.info("Before update for {}", definition.getName());
    }

    @Override
    public void afterUpdate(Item entity, CoredeuxEntityDefinition definition, OperationContext context) {
        touch(entity);
        LOG.info("After update for {}", definition.getName());
    }
}
```

The export cleanup hook shows how to target a very specific lifecycle task:

```java
@Component("exportStorageCleanupHook")
public class ExportStorageCleanupHook implements CoredeuxEntityHook<ExportStorageRecord> {

    @Override
    public void beforeDelete(ExportStorageRecord entity, CoredeuxEntityDefinition definition,
            OperationContext context) {
        if (entity == null) {
            return;
        }
        cleanupFilesystemArtifact(entity).ifPresent(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to delete export artifact at " + path.toAbsolutePath(),
                        exception);
            }
        });
    }
}
```

And the demo YAML enables them per entity:

```yaml
coredeux:
  entities:
    - full-class-name: com.coredeux.demo.domain.Customer
      modules:
        - name: hooks
          enabled: true
          handlers:
            - demoLifecycleHook
    - full-class-name: com.coredeux.demo.export.ExportStorageRecord
      modules:
        - name: hooks
          enabled: true
          handlers:
            - exportStorageCleanupHook
```

## YAML Configuration

Hooks are enabled per entity through the `hooks` module.

```yaml
modules:
  - name: hooks
    enabled: true
    handlers:
      - demoLifecycleHook
```

If a second hook is needed for a different entity type, add another bean and
reference it from that entity's module list.

## When To Use A Hook

Use a hook when the behavior is tied to the entity lifecycle itself:

- enrich the entity before it is saved
- stamp metadata after it is updated
- react when an entity is loaded
- clean up a resource before delete

If the behavior is really about validation, use a validator instead. If it is
about audit logging, use the audit module instead.

## Native Versus Spring

The hook contract is the same in both native and Spring Boot applications.

The only difference is how the bean is registered:

- Spring Boot: declare it as a bean or component
- native Java: register it in your bootstrap runtime or component registry

## What To Read Next

- [Add a Validator](09-add-validator.md)
- [Add Audit](10-add-audit.md)

<!-- docs-nav-start -->
[Previous: Available Data Access Implementations](07-available-data-access-implementations.md) | [Documentation Home](../../README.md) | [Next: Add a Validator](09-add-validator.md)
<!-- docs-nav-end -->
