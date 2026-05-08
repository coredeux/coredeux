# coredeux-core Reference

<!-- docs-nav-start -->
[Previous: Coredeux Core](README.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Adding A Core Module](add-core-module.md)
<!-- docs-nav-end -->

This document is the detailed reference for `coredeux-core`.

It is written for developers and agents who need enough detail to start using
or extending the module without reverse-engineering the source tree.

Use this document together with:

- [Architecture Overview](../../architecture/overview.md)
- [Lifecycle Model](../../architecture/lifecycle.md)
- [Entity Definitions](../../configuration/entity-definitions.md)
- [Module System](../../features/modules.md)

## Purpose

`coredeux-core` is the framework foundation of Coredeux.

It provides:

- the public service API used by application code
- the internal strategy layer that owns lifecycle orchestration
- the storage SPI used by persistence adapters
- the YAML-backed entity-definition model
- runtime context passed to validators, hooks, and audit handlers
- the built-in module execution model
- the core exception hierarchy

It does not provide a database implementation itself. That is the job of
modules such as `coredeux-core-jpa` and `coredeux-core-mongodb`.

## Core Flow

The current runtime flow is:

`CoredeuxService -> CoredeuxStrategy -> Modules -> CoredeuxDataAccessService`

Responsibility split:

- `CoredeuxService`
  Public application-facing API
- `CoredeuxStrategy`
  Lifecycle orchestration and routing
- `CoredeuxEntityModuleHandler`
  Per-module execution adapter
- `CoredeuxDataAccessService`
  Storage-specific persistence contract

## Package Structure

Main packages in `coredeux-core`:

- `com.coredeux.core.audit`
- `com.coredeux.core.config`
- `com.coredeux.core.context`
- `com.coredeux.core.definition`
- `com.coredeux.core.exceptions`
- `com.coredeux.core.helper`
- `com.coredeux.core.hooks`
- `com.coredeux.core.loader`
- `com.coredeux.core.module`
- `com.coredeux.core.registry`
- `com.coredeux.core.resolver`
- `com.coredeux.core.search`
- `com.coredeux.core.service`
- `com.coredeux.core.strategy`
- `com.coredeux.core.validation`

## Public Contracts

### `CoredeuxService`

File:
- [CoredeuxService.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/service/CoredeuxService.java)

This is the main application-facing service.

Methods:

- `load(String id, Class<T> type)`
- `query(String query, Map<String, Object> params, Class<T> type, int pageSize, int currentPage)`
- `loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage)`
- `supportedComparators(Class<?> type)`
- `save(T entity)`
- `update(T entity)`
- `remove(String id, Class<T> type)`
- `remove(T entity)`
- `refresh(T entity)`

Behavior notes:

- callers do not pass `OperationContext`
- lifecycle context is derived internally
- the public API is storage-agnostic

Default implementation:
- [DefaultCoredeuxService.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/service/impl/DefaultCoredeuxService.java)

This implementation is intentionally thin and delegates every operation to the
configured `CoredeuxStrategy`.

### `CoredeuxStrategy`

File:
- [CoredeuxStrategy.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/strategy/CoredeuxStrategy.java)

This is the internal contract that coordinates the full entity lifecycle.

Default implementation:
- [DefaultCoredeuxStrategy.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/strategy/impl/DefaultCoredeuxStrategy.java)

Shared base:
- [AbstractCoredeuxStrategy.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/strategy/impl/AbstractCoredeuxStrategy.java)

`AbstractCoredeuxStrategy` is responsible for:

- resolving the `CoredeuxEntityDefinition`
- resolving the `CoredeuxDataAccessService` bean for the entity
- extracting the identifier from the entity using the configured identifier field
- loading existing persisted state when needed
- creating `OperationContext`
- dispatching enabled modules

Important helper methods in `AbstractCoredeuxStrategy`:

- `getDefinition(Class<T> entityType)`
- `getDefinition(T entity)`
- `getDataAccessService(Class<T> entityType)`
- `executeModules(...)`
- `executeModule(...)`
- `createOperationContext(...)`
- `extractIdentifier(...)`
- `requireIdentifier(...)`
- `loadExistingEntity(...)`
- `requireExistingEntity(...)`

`DefaultCoredeuxStrategy` applies the actual lifecycle semantics:

- `load(...)`
  loads via data access, then invokes `hooks` with phase `load`
- `query(...)`
  loads a result page, then invokes `load` modules for each result row
- `loadAll(...)`
  same pattern as `query(...)`
- `supportedComparators(...)`
  returns the structured-search comparator names advertised by the data access
  service resolved for the entity type
- `save(...)`
  runs `CREATE` before persistence and `UPSERT` after persistence
- `update(...)`
  is strict `MODIFY`
- `remove(...)`
  is strict `DELETE`
- `refresh(...)`
  wraps refresh with refresh phases

### `CoredeuxDataAccessService`

File:
- [CoredeuxDataAccessService.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/service/CoredeuxDataAccessService.java)

This is the storage SPI.

Methods:

- `load`
- `save`
- `update`
- `remove`
- `loadAll`
- `supportedComparators`
- `query`
- `refresh`

Important boundary:

- `coredeux-core` owns lifecycle and module orchestration
- `CoredeuxDataAccessService` owns storage access only
- comparator support is a data-access capability. The core SPI exposes
  `supportedComparators(Class<?> type)` so tools, import files, and UI builders
  can ask the selected storage implementation which `SearchParams.comparator`
  values it understands.

### `CoredeuxModuleService`

File:
- [CoredeuxModuleService.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/service/CoredeuxModuleService.java)

This service lets callers execute configured modules outside normal CRUD flow.

Methods:

- `executeAll(T entity, String phase, String operation)`
- `executeModule(T entity, String moduleName, String phase, String operation)`

Default implementation:
- [DefaultCoredeuxModuleService.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/service/impl/DefaultCoredeuxModuleService.java)

Behavior notes:

- validates the request
- derives `OperationContext`
- resolves old state when the requested lifecycle operation requires it
- runs either all enabled modules or one named module
- uses existing state as the execution entity for `DELETE`

## Lifecycle Model

Constants file:
- [CoredeuxLifecycleOperations.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/strategy/CoredeuxLifecycleOperations.java)

Canonical operations:

- `CREATE`
- `MODIFY`
- `UPSERT`
- `DELETE`
- `FETCH`

These values are used across:

- strategy execution
- module invocation
- audit
- future import/export work

Current semantics:

- `save(entity)`
  - before persistence: `CREATE`
  - after persistence: `UPSERT`
- `update(entity)`
  - before and after update: `MODIFY`
- `remove(...)`
  - before delete: `DELETE`
- `load`, `query`, `loadAll`, `refresh`
  - `FETCH`

Hook phases are separate from lifecycle operations.

Hook phase constants file:
- [CoredeuxHookPhases.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/strategy/CoredeuxHookPhases.java)

Current phases:

- `load`
- `before-save`
- `after-save`
- `before-update`
- `after-update`
- `before-delete`
- `before-refresh`
- `after-refresh`

Important distinction:

- lifecycle operation is business/framework meaning
- phase is execution point within the strategy

## Runtime Context Model

### `RequestContext`

File:
- [RequestContext.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/context/RequestContext.java)

Fields:

- `requestId`
- `correlationId`
- `userId`
- `tenantId`
- `locale`

This is derived from the current servlet request when one exists.

Resolver:
- [DefaultCoredeuxRequestContextResolver.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/resolver/context/DefaultCoredeuxRequestContextResolver.java)

Resolution order for each supported key:

- request header
- request parameter
- request attribute

Supported keys:

- request id: `X-Request-Id`, `requestId`
- correlation id: `X-Correlation-Id`, `correlationId`
- user id: `X-User-Id`, `userId`
- tenant id: `X-Tenant-Id`, `tenantId`, `X-Site-Id`, `siteId`

If no servlet request exists, `resolve()` returns `null`.

### `EntityLifecycleContext<T>`

File:
- [EntityLifecycleContext.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/context/EntityLifecycleContext.java)

Fields:

- `operation`
- `identifier`
- `oldValue`
- `newValue`

This is the framework’s canonical description of the entity state transition
for one operation.

### `OperationContext`

File:
- [OperationContext.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/context/OperationContext.java)

Fields:

- `invokedAt`
- `requestContext`
- `lifecycleContext`

Factory/helper methods:

- `empty()`
- `withLifecycleContext(...)`

In normal Coredeux usage, callers do not create this object. The strategy does.

## YAML Entity Definition Model

Files:

- [CoredeuxYamlConfiguration.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/definition/CoredeuxYamlConfiguration.java)
- [CoredeuxEntityDefinition.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/definition/CoredeuxEntityDefinition.java)
- [CoredeuxStorageDefinition.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/definition/CoredeuxStorageDefinition.java)
- [CoredeuxModuleDefinition.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/definition/CoredeuxModuleDefinition.java)
- [CoredeuxAttributeDefinition.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/definition/CoredeuxAttributeDefinition.java)

Current root shape:

```yaml
coredeux:
  entities:
    - full-class-name: com.example.customer.Customer
      name: customer
      identifier: pk
      storage:
        store: postgres
        data-access-service: postgresCoredeuxJpaDataAccessService
      modules:
        - name: validators
          enabled: true
          handlers:
            - customerEmailValidator
        - name: hooks
          enabled: true
          handlers:
            - customerLifecycleHook
        - name: audit
          enabled: true
          handlers:
            - customerAuditHandler
          config:
            operations:
              - SAVE
              - UPDATE
              - DELETE
```

### `CoredeuxEntityDefinition`

Fields:

- `fullClassName`
- `name`
- `identifier`
- `storage`
- `modules`

Convenience methods:

- `getAudit()`
  Returns the configured `audit` module definition, or `null`
- `getValidators()`
  Returns enabled validator handler bean names
- `getHooks()`
  Returns enabled hook handler bean names
- `getAttributes()`
  Returns typed `attributes` module config if present
- `getModule(String moduleName)`
  Returns a matching module or `null`
- `getModuleDefinition(String moduleName)`
  Returns `Optional<CoredeuxModuleDefinition>`

Important note:

- registry lookup is based on `fullClassName`
- `name` is not treated as a unique identity key by the core registry

### `CoredeuxStorageDefinition`

Fields:

- `store`
- `dataAccessService`

`dataAccessService` is the key field used by the default data-access resolver.

### `CoredeuxModuleDefinition`

Fields:

- `name`
- `enabled`
- `handlers`
- `config`

Helper:

- `getConfigMap()`
  returns `config` as `Map<String, Object>` when possible, otherwise empty map

### `CoredeuxAttributeDefinition`

This is currently used when the optional `attributes` module is present.

Fields:

- `name`
- `type`
- `required`
- `searchable`
- `validators`

## YAML Loading and Registry

### Loader

Files:

- [EntityDefinitionLoader.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/loader/EntityDefinitionLoader.java)
- [YamlEntityDefinitionLoader.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/loader/YamlEntityDefinitionLoader.java)

`YamlEntityDefinitionLoader`:

- reads from `InputStream` or `Path`
- expects root key `coredeux`
- expects `entities` to be a list
- validates required fields
- defaults `name` to `fullClassName` when omitted
- converts module config recursively into immutable `Map` or `List`
- treats the special module name `attributes` as typed attribute config

Validation rules enforced today:

- YAML document must not be empty
- `full-class-name` is required
- `identifier` is required
- `storage` is required
- `storage.data-access-service` is required
- module `name` is required

### Registry

Files:

- [EntityDefinitionRegistry.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/registry/EntityDefinitionRegistry.java)
- [InMemoryEntityDefinitionRegistry.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/registry/InMemoryEntityDefinitionRegistry.java)
- [EntityDefinitionRegistries.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/registry/EntityDefinitionRegistries.java)

`EntityDefinitionRegistry` methods:

- `findByFullClassName(...)`
- `findByEntityType(...)`
- `getAll()`

`InMemoryEntityDefinitionRegistry`:

- stores definitions keyed by `fullClassName`
- rejects duplicate entity definitions for the same class

`EntityDefinitionRegistries`:

- convenience factory for creating registries from YAML path/input stream

### Spring Boot configuration

File:
- [CoredeuxEntityDefinitionConfiguration.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/config/CoredeuxEntityDefinitionConfiguration.java)

Bean exposed:

- `EntityDefinitionRegistry entityDefinitionRegistry(...)`

Config property:

- `coredeux.entities.config-location`

Default value:

- `classpath:coredeux-entities.yml`

## Data-Access Resolution

Files:

- [EntityDataAccessResolver.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/resolver/EntityDataAccessResolver.java)
- [EntityDefinitionBackedDataAccessResolver.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/resolver/EntityDefinitionBackedDataAccessResolver.java)

Default behavior:

- read `definition.storage.dataAccessService`
- return that Spring bean name
- fail with `CoredeuxValidationException` if missing

This keeps the core persistence routing rule very simple and easy to replace if
needed later.

## Module System in core

### Base module handler contract

File:
- [CoredeuxEntityModuleHandler.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/module/CoredeuxEntityModuleHandler.java)

Methods:

- `getModuleName()`
- `execute(...)`

Strategy behavior:

- module execution only happens for enabled modules
- the framework resolves one `CoredeuxEntityModuleHandler` per module name
- duplicate module handlers for the same name are rejected at strategy startup
- missing handlers are ignored, not treated as errors

### Validators

Files:

- [CoredeuxEntityValidator.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/validation/CoredeuxEntityValidator.java)
- [ValidatorsModuleHandler.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/module/impl/ValidatorsModuleHandler.java)
- [ValidationError.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/validation/ValidationError.java)

Validator contract:

- `List<ValidationError> validate(T entity, CoredeuxEntityDefinition definition, OperationContext context)`

Execution rules:

- only runs for `before-save` and `before-update`
- blank handler names are ignored
- all validation errors are aggregated
- if any errors are returned, a `CoredeuxValidationException` is thrown

Generic typing:

- validators are typed as `CoredeuxEntityValidator<T>`
- the handler uses `ResolvableType` to validate bean compatibility at runtime
- mismatched validator types fail fast with `CoredeuxStrategyException`

### Hooks

Files:

- [CoredeuxEntityHook.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/hooks/CoredeuxEntityHook.java)
- [HooksModuleHandler.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/module/impl/HooksModuleHandler.java)

Hook methods:

- `onLoad`
- `beforeSave`
- `afterSave`
- `beforeUpdate`
- `afterUpdate`
- `beforeDelete`
- `beforeRefresh`
- `afterRefresh`

Execution rules:

- one hook bean can implement only the phases it needs
- blank handler names are ignored
- unsupported phase values cause `CoredeuxStrategyException`
- runtime generic type compatibility is validated

### Audit

Files:

- [CoredeuxEntityAuditHandler.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/audit/CoredeuxEntityAuditHandler.java)
- [AuditModuleHandler.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/module/impl/AuditModuleHandler.java)

Audit contract:

- `void audit(T entity, CoredeuxEntityDefinition definition, OperationContext context)`

Current audit config shape:

```yaml
- name: audit
  enabled: true
  handlers:
    - customerAuditHandler
  config:
    operations:
      - SAVE
      - UPDATE
      - DELETE
```

Supported configured operations:

- `SAVE`
- `UPDATE`
- `DELETE`
- `ALL`

Current phase-to-operation mapping:

- `after-save` -> `SAVE`
- `after-update` -> `UPDATE`
- `before-delete` -> `DELETE`

Validation rules:

- `config.operations` is required
- operations must be one of the supported values
- runtime generic type compatibility is validated

## Reflection Helper

Files:

- [CoredeuxReflectionHelperService.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/helper/CoredeuxReflectionHelperService.java)
- [DefaultCoredeuxReflectionHelperService.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/helper/impl/DefaultCoredeuxReflectionHelperService.java)

This helper centralizes the reflection behavior needed by the framework today.

Use cases:

- identifier extraction
- field traversal through inheritance
- field value read/write
- generic field discovery

It exists to keep reflection logic out of the strategy and to provide one place
for future type or field utilities.

## Search Model

Files:

- [SearchParams.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/search/SearchParams.java)
- [SearchResult.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/search/SearchResult.java)
- [PaginationData.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/search/PaginationData.java)

### `SearchParams`

Fields:

- `field`
- `comparator`
- `value`

Representative JSON shape:

```json
{
  "field": "status",
  "comparator": "EQUALS",
  "value": "ACTIVE"
}
```

### `SearchResult<T>`

Fields:

- `results`
- `pagination`

Representative JSON shape:

```json
{
  "results": [
    {
      "pk": 1,
      "name": "Charlie"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalResults": 10,
    "pageSize": 10,
    "totalPages": 1,
    "resultSize": 10
  }
}
```

### `PaginationData`

Fields:

- `currentPage`
- `totalResults`
- `pageSize`
- `totalPages`
- `resultSize`

Important note:

- these JSON shapes are common serialized forms when Coredeux is exposed via REST
- `coredeux-core` itself does not define a REST API

## Exception Hierarchy

Files:

- [CoredeuxCoreException.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/exceptions/CoredeuxCoreException.java)
- [CoredeuxDataAccessException.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/exceptions/CoredeuxDataAccessException.java)
- [CoredeuxStrategyException.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/exceptions/CoredeuxStrategyException.java)
- [CoredeuxValidationException.java](../../../modules/coredeux-core/src/main/java/com/coredeux/core/exceptions/CoredeuxValidationException.java)

Usage guidance:

- use `CoredeuxValidationException` for bad caller/configuration/state input
- use `CoredeuxStrategyException` for framework orchestration problems
- use `CoredeuxDataAccessException` for persistence adapter failures

## Spring Beans You Get

When `coredeux-core` is on the classpath in a Spring Boot application, the main
framework beans available are:

- `entityDefinitionRegistry`
- `DefaultCoredeuxStrategy`
- `DefaultCoredeuxService`
- `DefaultCoredeuxModuleService`
- `EntityDefinitionBackedDataAccessResolver`
- `DefaultCoredeuxRequestContextResolver`
- `YamlEntityDefinitionLoader`
- `ValidatorsModuleHandler`
- `HooksModuleHandler`
- `AuditModuleHandler`
- `DefaultCoredeuxReflectionHelperService`

## How To Use `coredeux-core`

Minimum requirements for an application:

1. add `coredeux-core`
2. provide `coredeux-entities.yml`
3. add at least one `CoredeuxDataAccessService` implementation module
4. inject `CoredeuxService`

Minimal example:

```java
@Service
public class CustomerApplicationService {

    private final CoredeuxService coredeuxService;

    public CustomerApplicationService(CoredeuxService coredeuxService) {
        this.coredeuxService = coredeuxService;
    }

    public String create(Customer customer) {
        return coredeuxService.save(customer);
    }

    public Customer find(String id) {
        return coredeuxService.load(id, Customer.class);
    }
}
```

## Current Boundaries

What `coredeux-core` intentionally does not do today:

- provide persistence implementation details
- expose a REST API
- define GraphQL read shaping
- force fetch/eager-loading policies into the core abstraction
- manage DTO mapping

Those concerns belong in adapter modules or transport modules, not the core.

## Recommended Reading Order

If you are new to Coredeux and want to work quickly:

1. read this file
2. read [Entity Definitions](../../configuration/entity-definitions.md)
3. read [Module System](../../features/modules.md)
4. read the JPA reference if your project uses JPA

<!-- docs-nav-start -->
[Previous: Coredeux Core](README.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Adding A Core Module](add-core-module.md)
<!-- docs-nav-end -->
