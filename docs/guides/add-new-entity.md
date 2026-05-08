# Adding A New Entity

<!-- docs-nav-start -->
[Previous: Coredeux Demo Tour](demo-tour.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Modules](../modules/README.md)
<!-- docs-nav-end -->

This guide shows the practical checklist for making a new entity work across
Coredeux CRUD, modules, import, import files, and export.

## 1. Create The Entity Class

Create the entity class in your application or feature module.

The framework needs a stable identifier field that matches the configured
`identifier` value in YAML. With JPA, the identifier can still use normal JPA
annotations; Coredeux only needs to know which field represents identity.

## 2. Choose A Data Access Service

Pick the `CoredeuxDataAccessService` bean that should handle persistence for the
entity.

Current examples:

- `defaultCoredeuxJpaDataAccessService`
- `postgresCoredeuxJpaDataAccessService`
- `defaultCoredeuxMongoDataAccessService`

Use the PostgreSQL adapter when the entity needs PostgreSQL-specific JSONB
query behavior. Use the generic JPA adapter when normal JPA/Criteria behavior
is enough. Use the MongoDB adapter when the entity is document-oriented and you
want Mongo-backed search comparators and query semantics.

## 3. Add The YAML Definition

Add the entity to `coredeux-entities.yml`, or to the resource configured through
`coredeux.entities.config-location`.

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
- custom modules such as workflow or future agent-aware/MCP capabilities

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
- import value handlers for unsupported or application-specific field types
- export value handlers for custom formatting, masking, labels, or redaction

The import parser module itself compiles files into `ImportRequest`; it does
not need entity-specific parser beans unless your application adds custom file
handling around it.

## 6. Use The Public Service

Application code should interact with `CoredeuxService` rather than directly
orchestrating module execution or persistence routing.

Minimum operations to verify:

- `save(entity)`
- `load(id, type)`
- `loadAll(filters, type, pageSize, currentPage)`
- `update(entity)`
- `remove(id, type)` or `remove(entity)`

The demo exposes the same model through `/api/entities/{entityName}` endpoints,
where `entityName` is the fully qualified Java class name.

## 7. Check Import Readiness

If the entity should be imported, decide how existing records are identified.

Common strategies:

- `CREATE`: no existing-record strategy required
- `UPSERT`, `MODIFY`, `DELETE`, `FETCH`: use exactly one of `unique`, `lookup`,
  or `query`

Example import-file header:

```text
UPSERT com.example.customer.Customer | email(unique=true) | name | status(default=ACTIVE)
```

For relationships, decide whether imported values use:

- direct values
- `reference=field`
- compound references such as `reference=tenant:code`
- row-key references with `reference=*`

For unsupported target types, add a `CoredeuxImportValueHandler` and reference
it from the import column.

## 8. Check Export Readiness

If the entity should be exported, decide which field paths are safe and useful.

Simple fields use their field name:

```json
{ "path": "email" }
```

Nested object or collection paths use compact colon syntax:

```json
{ "path": "profile:displayName" }
```

Use a custom `CoredeuxExportValueHandler` when fields need masking, formatting,
localization, labels, or redaction.

## 9. Test The Entity Flow

At minimum, verify:

- the entity definition loads successfully
- the configured data access service is resolved
- `CoredeuxService` CRUD/search operations work
- validators, hooks, audit, and custom modules execute as expected
- raw JSON import validates and executes when import is supported
- text or Excel import files parse and validate when file import is supported
- export queues a job, completes, and stores a retrievable artifact when export
  is supported

Useful module-level test commands:

```powershell
mvn -pl modules/coredeux-core -am test
mvn -pl modules/coredeux-import -am test
mvn -pl modules/coredeux-import-parser -am test
mvn -pl modules/coredeux-export -am test
mvn -pl examples/coredeux-demo -am test
```

## What To Read Next

- [Entity Definitions](../configuration/entity-definitions.md)
- [Module System](../features/modules.md)
- [Raw JSON Import Tutorial](../modules/import/guide.md)
- [Import File Tutorial](../modules/import-parser/guide.md)
- [Export Guide](../modules/export/guide.md)

<!-- docs-nav-start -->
[Previous: Coredeux Demo Tour](demo-tour.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Modules](../modules/README.md)
<!-- docs-nav-end -->
