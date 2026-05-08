# Lifecycle Model

<!-- docs-nav-start -->
[Previous: Architecture Overview](overview.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Configuration](../configuration/README.md)
<!-- docs-nav-end -->

Coredeux uses a small framework-wide lifecycle vocabulary so all services, modules, and future capabilities work from the same semantics.

## Canonical Operations

The current lifecycle operations are:

- `CREATE`
- `MODIFY`
- `UPSERT`
- `DELETE`
- `FETCH`

These values are exposed through `EntityLifecycleContext` and are intended to remain stable so import, export, workflow, and other feature modules can depend on the same operation model.

## Why Lifecycle Exists

The framework does not want callers to manually load old values, build context, or decide which modules should run.

Instead, Coredeux derives lifecycle state internally and passes it to validators, hooks, audit handlers, and future modules.

## Operation Semantics

### Save

`save(entity)` currently has two framework moments:

- before persistence: `CREATE`
- after persistence: `UPSERT`

This means modules can distinguish the intent before save from the post-persistence state after save.

When possible, the framework attempts to load the existing persisted state before save so `oldValue` can still be present in lifecycle context.

### Update

`update(entity)` uses `MODIFY`.

This is a strict update path. The framework resolves the current persisted state and expects the entity to already exist.

### Remove

`remove(entity)` and `remove(id, type)` use `DELETE`.

The framework resolves the persisted entity before delete so modules can work against the stored state.

### Fetch

`load`, `loadAll`, `query`, and `refresh` use `FETCH`.

These operations represent framework-managed retrieval and refresh flows.

## Old And New Values

Coredeux derives `oldValue` and `newValue` itself.

The caller is not expected to pass them.

General rules:

- create-like flow
  - `oldValue` may be `null`
  - `newValue` is the incoming entity
- modify flow
  - `oldValue` is the currently persisted state
  - `newValue` is the incoming entity
- delete flow
  - `oldValue` is the currently persisted state
  - `newValue` is typically `null`
- fetch flow
  - lifecycle is read-oriented and entity state is made available through the current execution context

## Request Context And Operation Context

The framework builds runtime context for module execution.

### RequestContext

`RequestContext` holds request-scoped information when a servlet request is available. Typical values include:

- site or tenant information
- request identifiers
- request metadata derived from headers, parameters, or attributes

### OperationContext

`OperationContext` is the framework-owned runtime container passed to modules.

It currently includes:

- `invokedAt`
- `requestContext`
- `lifecycleContext`

### EntityLifecycleContext

`EntityLifecycleContext` contains the lifecycle-specific values:

- `operation`
- `identifier`
- `oldValue`
- `newValue`

## Module Execution

Lifecycle is what allows module execution to stay consistent.

Examples:

- validators can inspect the incoming entity and lifecycle intent
- hooks can react to framework phases while still reading the lifecycle operation
- audit handlers can read operation, identifier, old value, and new value without application code constructing an audit payload manually

## Internal Snapshot Loading

When the framework needs the current persisted state, it loads it directly from `CoredeuxDataAccessService` without re-triggering modules.

That is intentional.

The old value is treated as a persisted snapshot used to derive lifecycle state, not as another public fetch flow.

## Stability Guidance

New framework features should reuse the same lifecycle vocabulary and derive their behavior from `EntityLifecycleContext` rather than inventing a separate operation model.

<!-- docs-nav-start -->
[Previous: Architecture Overview](overview.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Configuration](../configuration/README.md)
<!-- docs-nav-end -->
