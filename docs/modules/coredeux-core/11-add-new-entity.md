# Adding a New Entity

<!-- docs-nav-start -->
[Previous: Add Audit](/10-add-audit) | [Documentation Home](/) | [Next: Adding a Core Module](/12-add-core-module)
<!-- docs-nav-end -->

This guide shows the practical checklist for making a new entity work across
Coredeux CRUD and core module behavior.

## 1. Create The Entity Class

Create the entity class in your application or feature module.

The framework needs a stable identifier field that matches the configured
`identifier` value in YAML. With JPA, the identifier can still use normal JPA
annotations; Coredeux only needs to know which field represents identity.

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
through `coredeux.entities.config-location`.

```yaml
coredeux:
  entities:
    - full-class-name: com.example.customer.Customer
      name: customer
      identifier: id
      storage:
        data-access-service: defaultCoredeuxJpaDataAccessService
```

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

- [Entity Definitions](03-entity-definitions.md)
- [Module System](05-modules.md)
- [Add Or Choose A Data Access Service](06-add-data-access-service.md)
- [Add a Hook](08-add-hook.md)
- [Add a Validator](09-add-validator.md)
- [Add Audit](/10-add-audit)

<!-- docs-nav-start -->
[Previous: Add Audit](/10-add-audit) | [Documentation Home](/) | [Next: Adding a Core Module](/12-add-core-module)
<!-- docs-nav-end -->
