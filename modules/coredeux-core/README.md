# coredeux-core

`coredeux-core` contains the framework contracts and orchestration model used by the Coredeux platform.

This module is the foundation that other Coredeux modules depend on.

Detailed reference:

- [docs/modules/core/reference.md](/C:/Data/Development/Coredeux/coredeux-oss/coredeux/docs/modules/core/reference.md)

## What This Module Provides

`coredeux-core` provides:

- the public framework service contract: `CoredeuxService`
- lifecycle orchestration through `CoredeuxStrategy`
- the persistence SPI: `CoredeuxDataAccessService`
- entity-level module execution through `CoredeuxModuleService`
- YAML-backed entity definitions and registry/resolver support
- framework context objects such as `RequestContext`, `OperationContext`, and `EntityLifecycleContext`
- extension contracts for validators, hooks, and audit handlers
- framework exception hierarchy

## Layering

The current layering is:

`CoredeuxService -> CoredeuxStrategy -> Modules -> CoredeuxDataAccessService`

### `CoredeuxService`

This is the public API intended for business/application code.

### `CoredeuxStrategy`

This is the internal framework orchestration layer. It is responsible for:

- resolving the entity definition
- extracting identifiers
- loading existing persisted state when needed
- deriving request and lifecycle context
- executing configured modules
- delegating to the correct `CoredeuxDataAccessService`

### `CoredeuxDataAccessService`

This is the SPI implemented by storage-specific modules such as JPA, MongoDB, or future adapters.

## Lifecycle Model

The canonical lifecycle operations used across the framework are:

- `CREATE`
- `MODIFY`
- `UPSERT`
- `DELETE`
- `FETCH`

These values are intended to remain consistent throughout the framework so future capabilities like impex and audit can rely on one lifecycle vocabulary.

The framework derives lifecycle information itself. Developers are not expected to manually provide old values or framework context.

### Current semantics

- `save(entity)`
  runs with `CREATE` before persistence and `UPSERT` after persistence
- `update(entity)`
  runs with `MODIFY`
- `remove(entity)` / `remove(id, type)`
  runs with `DELETE`
- `load`, `loadAll`, `query`, `refresh`
  run with `FETCH`

## Entity Definitions

Entity definitions are loaded from YAML and stored in the framework registry.

Core entity attributes are:

- `full-class-name`
- `name`
- `identifier`
- `storage`

Modules are configured per entity.

Example:

```yaml
coredeux:
  entities:
    - full-class-name: com.example.customer.Customer
      name: customer
      identifier: id
      storage:
        data-access-service: defaultCoredeuxJpaDataAccessService
      modules:
        - name: validators
          enabled: true
          handlers:
            - customerRequiredFieldsValidator
```

If `name` is not provided, it defaults to `fullClassName` during YAML loading.

## Coredeux Properties

`coredeux-core` also loads a generic `META-INF/coredeux.yml` file for runtime
configuration.

The loader keeps the YAML as a nested property map so the core module does not
need to know anything about feature-module keys such as import or export.

Typical usage in a native Java app is:

```yaml
coredeux:
  entities:
    config-location: classpath:coredeux-postgres-entities.yml
  import:
    default-parser: text
  export:
    default-format: TEXT
```

Applications and feature modules can read the map and decide how to interpret
their own keys. Spring-based starters can merge this map with environment or
application properties before handing it to the framework.

## Built-In Module Types

### `validators`

Validators implement `CoredeuxEntityValidator` and return a list of `ValidationError`.

They currently execute before save/update.

### `hooks`

Hooks implement `CoredeuxEntityHook` and receive lifecycle callbacks at the configured framework phase.

### `audit`

Audit handlers implement `CoredeuxEntityAuditHandler`.

Audit configuration uses business-level operations rather than hook phases:

```yaml
- name: audit
  enabled: true
  handlers:
    - defaultAuditHandler
  config:
    operations:
      - SAVE
      - UPDATE
      - DELETE
```

The framework maps these operations internally to the actual lifecycle execution points.

## External Module Execution

`CoredeuxModuleService` allows modules to be invoked outside normal CRUD flow.

This is useful for cases such as:

- re-running configured modules on an existing entity
- admin tools
- import/export flows
- future replay/reprocessing scenarios

The framework still derives lifecycle context internally when this service is used.

## Spring Integration

`coredeux-core` exposes Spring-managed components for:

- YAML loading
- entity definition registry creation
- data access resolver wiring
- strategy and service implementations
- module handlers

A typical application will place entity definitions on the classpath and inject the public framework services.

## Key Extension Points

Core extension contracts in this module include:

- `CoredeuxDataAccessService`
- `CoredeuxEntityValidator`
- `CoredeuxEntityHook`
- `CoredeuxEntityAuditHandler`
- `CoredeuxEntityModuleHandler`

Feature modules should implement these contracts rather than redefining them.

## Who Should Depend On This Module

Depend on `coredeux-core` if you are:

- implementing a new persistence adapter
- writing validators, hooks, or audit handlers
- using Coredeux service/strategy contracts in an application
- building another Coredeux feature module on top of the framework core
