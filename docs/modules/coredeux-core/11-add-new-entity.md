# Adding a New Entity

<!-- docs-nav-start -->
[Previous: Add Audit](/modules/coredeux-core/10-add-audit) | [Documentation Home](/) | [Next: Adding a Core Module](/modules/coredeux-core/12-add-core-module)
<!-- docs-nav-end -->

This guide shows the practical checklist for making a new entity work across
Coredeux CRUD and core module behavior.

## 1. Create The Entity Class

Create the entity class in your application or feature module.

The framework needs a stable identifier source for the entity. You can define
it at the entity level with `identifier`, or at the storage level with
`storage.identifier` when several entities share the same backend contract.
If neither is present, Coredeux can fall back to the environment-level
`coredeux.identifier` value. Coredeux does not infer the identifier from field
names.

With JPA, the identifier can still use normal JPA annotations; Coredeux only
needs the explicit YAML contract.

## 2. Choose A Data Access Service

Pick the `CoredeuxDataAccessService` bean that should handle persistence for
the entity.

Current examples:

- `defaultCoredeuxJpaDataAccessService`
- `postgresCoredeuxJpaDataAccessService`
- `defaultCoredeuxMongoDataAccessService`

Use the PostgreSQL adapter when the entity needs PostgreSQL-specific JSONB
query behavior. Use the generic JPA adapter when normal JPA or Criteria
behavior is enough. Use the MongoDB adapter when the entity is document-oriented
and you want Mongo-backed search comparators and query semantics.

## 3. Add The YAML Definition

Add the entity to `coredeux-entities.yml`, or to the resource configured
through `coredeux.entities.config-location` if you need entity-specific
overrides or modules. If the entity can use the global fallback data access
service, it can also run without an explicit entity entry.

```yaml
coredeux:
  entities:
    - full-class-name: com.example.customer.Customer
      name: customer
      storage:
        identifier: id
        data-access-service: defaultCoredeuxJpaDataAccessService
```

If you omit `storage.data-access-service`, Coredeux uses the global
`coredeux.data-access-service` setting.

At this stage, `CoredeuxService` can load, list, save, update, and remove the
entity if the data access service can persist it.

## 4. Add Optional Modules

Depending on the entity, enable:

- `validators` for business validation
- `hooks` for lifecycle callbacks
- `audit` for business operation audit events
- custom modules such as workflow or future agent-aware or MCP capabilities

Each module refers to Spring bean names through `handlers`.

```yaml
modules:
  - name: validators
    enabled: true
    handlers:
      - customerRequiredFieldsValidator
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

## 5. Ensure Beans Are Registered

Make sure the following are available as Spring beans where needed:

- the chosen `CoredeuxDataAccessService`
- validator beans
- hook beans
- audit beans
- custom module handlers

## 6. Use The Public Service

Application code should interact with `CoredeuxService` rather than directly
orchestrating module execution or persistence routing.

Minimum operations to verify:

- `save(entity)`
- `load(id, type)`
- `loadAll(filters, type, pageSize, currentPage)`
- `update(entity)`
- `remove(id, type)` or `remove(entity)`

The demo exposes the same model through `/api/entities/{entityName}`
endpoints, where `entityName` is the fully qualified Java class name.

## 7. Test The Entity Flow

At minimum, verify:

- the entity definition loads successfully
- the configured data access service is resolved
- `CoredeuxService` CRUD and search operations work
- validators, hooks, audit, and custom modules execute as expected

Useful module-level test commands:

```powershell
mvn -pl modules/coredeux-core -am test
```

## What To Read Next

- [Entity Definitions](/modules/coredeux-core/03-entity-definitions)
- [Module System](/modules/coredeux-core/05-modules)
- [Add Or Choose A Data Access Service](/modules/coredeux-core/06-add-data-access-service)
- [Add a Hook](/modules/coredeux-core/08-add-hook)
- [Add a Validator](/modules/coredeux-core/09-add-validator)
- [Add Audit](/modules/coredeux-core/10-add-audit)

<!-- docs-nav-start -->
[Previous: Add Audit](/modules/coredeux-core/10-add-audit) | [Documentation Home](/) | [Next: Adding a Core Module](/modules/coredeux-core/12-add-core-module)
<!-- docs-nav-end -->
