# Coredeux Demo Tour

<!-- docs-nav-start -->
[Previous: Docker Demo Setup](docker-demo.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Adding A New Entity](add-new-entity.md)
<!-- docs-nav-end -->

The `coredeux-demo` application is more than a smoke test. It is a compact
example application that shows how to extend Coredeux without changing the core
framework.

Read this after [First 10 Minutes With Coredeux](first-10-minutes.md). The first
guide gets the app running; this page explains what the demo is teaching.

If you want a containerized local setup, start with
[Docker Demo Setup](docker-demo.md). It covers the Docker install steps for
Windows, macOS, and Linux, then brings up the supporting PostgreSQL, MongoDB,
Redis, and Elasticsearch services with the demo application.

## Demo Structure

The demo lives under:

```text
examples/coredeux-demo
```

Important areas:

- `src/main/resources/application.yml`: database, entity-definition, export,
  worker, and Swagger configuration
- `src/main/resources/coredeux-entities.yml`: managed entity definitions and
  enabled modules
- `src/main/java/com/coredeux/demo/domain`: JPA model used by the demo
- `src/main/java/com/coredeux/demo/domain/jdbc`: JDBC-backed example entity
- `src/main/java/com/coredeux/demo/domain/mongodb`: MongoDB document example
- `src/main/java/com/coredeux/demo/domain/elasticsearch`: Elasticsearch
  document example
- `src/main/java/com/coredeux/demo/domain/redis`: Redis-backed example entity
- `src/main/java/com/coredeux/demo/web`: generic CRUD, import, file import, and
  export controllers
- `src/main/java/com/coredeux/demo/validation`: validator examples
- `src/main/java/com/coredeux/demo/hooks`: lifecycle hook examples
- `src/main/java/com/coredeux/demo/audit`: audit handler example
- `src/main/java/com/coredeux/demo/imports`: custom import value handlers
- `src/main/java/com/coredeux/demo/export`: custom export storage backend
- `src/main/java/com/coredeux/demo/workflow`: demo custom module
- `src/main/java/com/coredeux/demo/bootstrap`: startup data and JSONB search
  examples

## Entity Definitions

The central demo configuration is
[coredeux-entities.yml](../../examples/coredeux-demo/src/main/resources/coredeux-entities.yml).

It registers entities such as:

- `Customer`
- `Product`
- `CustomerOrder`
- `Role`
- `Address`
- `CustomerProfile`
- `OrderItem`
- `ExportStorageRecord`

Most demo entities use:

```yaml
storage:
  data-access-service: postgresCoredeuxJpaDataAccessService
```

This intentionally exercises the PostgreSQL JPA strategy, including JSONB-aware
query behavior.

The multi-database examples use the same generic CRUD controller and simply
point `coredeux-entities.yml` at a different data-access service:

- `com.coredeux.demo.domain.jdbc.JdbcInventoryItem` ->
  `defaultCoredeuxJdbcDataAccessService`
- `com.coredeux.demo.domain.mongodb.MongoAuditTrail` ->
  `defaultCoredeuxMongoDataAccessService`
- `com.coredeux.demo.domain.elasticsearch.ElasticsearchCatalogEntry` ->
  `defaultCoredeuxElasticsearchDataAccessService`
- `com.coredeux.demo.domain.redis.RedisSessionSnapshot` ->
  `defaultCoredeuxRedisDataAccessService`

That is the clean demo of Coredeux's versatility: the controller stays the same,
the entity class changes, and the YAML decides which backend is used.

## Generic CRUD Controller

[CoredeuxCrudController.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/web/CoredeuxCrudController.java)
shows how to expose one generic CRUD controller over any Coredeux-managed entity.

It demonstrates:

- resolving an entity definition from the path
- resolving the Java class from the fully qualified class name
- converting JSON payloads into typed entity instances
- calling `CoredeuxService.save`, `load`, `loadAll`, `update`, and `remove`
- applying the typed identifier during update

This is not required by Coredeux, but it is a useful pattern for admin APIs,
internal tools, generated back offices, and future agent-facing operations.

## Validation Extension

[CustomerEmailValidator.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/validation/CustomerEmailValidator.java)
implements `CoredeuxEntityValidator<Customer>`.

It validates:

- customer name
- email shape
- status presence

The validator is enabled from YAML:

```yaml
- name: validators
  enabled: true
  handlers:
    - customerEmailValidator
```

This shows the normal pattern for entity-specific business validation: write a
Spring bean, then reference the bean name from the entity definition.

## Lifecycle Hooks

[DemoLifecycleHook.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/hooks/DemoLifecycleHook.java)
implements `CoredeuxEntityHook<Item>`.

It demonstrates hook behavior across multiple entity types:

- logs load operations
- updates `Customer.lastLifecycleTouch`
- initializes `CustomerOrder.createdAt` when needed
- adds `lastHookTouch` to `Product.metadata`

The same hook can be attached to more than one entity because the handler type
is broad enough for the demo model.

## Audit Handler

[DemoAuditHandler.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/audit/DemoAuditHandler.java)
implements `CoredeuxEntityAuditHandler<Item>`.

It records in-memory audit entries containing:

- entity name
- lifecycle operation
- identifier

The YAML configuration decides which business operations trigger audit:

```yaml
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

This is deliberately simple. A real application could persist the audit event,
publish it, or forward it into an observability pipeline.

## Custom Workflow Module

The demo includes a custom module named `workflows`.

Key files:

- [WorkflowsModuleHandler.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/workflow/WorkflowsModuleHandler.java)
- [CoredeuxDemoWorkflowHandler.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/workflow/CoredeuxDemoWorkflowHandler.java)
- [DemoCustomerApprovalWorkflow.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/workflow/DemoCustomerApprovalWorkflow.java)

The module handler implements `CoredeuxEntityModuleHandler` and returns:

```java
"workflows"
```

That value matches the YAML module name:

```yaml
- name: workflows
  enabled: true
  handlers:
    - customerApprovalWorkflow
  config:
    phases:
      - after-save
```

The handler reads `config.phases`, resolves configured workflow beans from the
Spring context, validates generic handler type compatibility, and invokes the
workflow only for matching lifecycle phases.

This is one of the most important demo examples. It shows how applications can
add a new Coredeux capability without changing `coredeux-core`.

The same pattern is the likely shape for future framework-level extensions,
including MCP and agent-aware capabilities.

## Import Value Handlers

The demo includes custom import value handlers for field types that need
application-specific conversion.

If you want the request shape that uses these handlers, see
[Coredeux Raw JSON Import Tutorial](../modules/import/guide.md).

[DemoUriImportHandler.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/imports/DemoUriImportHandler.java)
implements `CoredeuxImportValueHandler` and converts a string into an absolute
`URI`.

It is used from import column metadata:

```text
documentationUrl(handler=demoUriImportHandler)
```

[LegacyDateImportHandler.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/imports/LegacyDateImportHandler.java)
parses `java.util.Date` values from a configured date format:

```text
legacySignupDate(handler=legacyDateImportHandler,metadata.dateFormat=dd/MM/yyyy)
```

These handlers show the import extension model:

- the import service owns row processing
- the column config chooses a handler
- the handler receives `ImportValueContext`
- metadata can carry application-specific parser instructions

## Import Controllers And Parser Use

[CoredeuxImportController.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/web/CoredeuxImportController.java)
exposes raw JSON import:

- `POST /api/import/validate`
- `POST /api/import`

[CoredeuxImportFileController.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/web/CoredeuxImportFileController.java)
exposes text and Excel file import:

- `POST /api/import/file/validate`
- `POST /api/import/file`

For the raw request format behind the controller, see
[Coredeux Raw JSON Import Tutorial](../modules/import/guide.md).
For the file format behind the file controller, see
[Coredeux Import File Tutorial](../modules/import-parser/guide.md).

The file controller chooses the parser from content type or filename:

- text source uses `CoredeuxTextImportParser`
- `.xls` or `.xlsx` source uses `CoredeuxExcelImportParser`

The parser compiles the upload into `ImportRequest`. The import service still
owns validation and execution.

## Export Storage Extension

The demo uses a custom database-backed export storage service:

For the broader export module, see [Coredeux Export Guide](../modules/export/guide.md).

[DatabaseCoredeuxExportStorageService.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/export/DatabaseCoredeuxExportStorageService.java)

It implements `CoredeuxExportStorageService` and stores generated export files
in the database as `ExportStorageRecord` rows.

The service returns an `ExportStorageArtifact` with a demo download URL:

```text
/api/export/{uid}/download
```

This shows how applications can replace the default filesystem storage with
database storage, cloud object storage, tenant-aware storage, or another custom
artifact backend.

## Export Cleanup Hook

[ExportStorageCleanupHook.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/hooks/ExportStorageCleanupHook.java)
is attached to `ExportStorageRecord`.

It demonstrates cleanup before delete:

- database-backed records do not need external cleanup
- filesystem-backed records can point to an artifact path
- the hook deletes the file before the storage row is removed

This example connects core lifecycle hooks with export storage lifecycle.

## Export Controller

[CoredeuxExportController.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/web/CoredeuxExportController.java)
shows the typical export API shape:

- queue an export request
- poll export status by uid
- download the stored artifact

The controller returns `202 Accepted` while jobs are `NEW` or `IN_PROGRESS`,
then returns the completed response once the worker finishes.

## Bootstrap Data And JSONB Search

[DemoDataRunner.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/bootstrap/DemoDataRunner.java)
loads sample data at startup.

It demonstrates:

- saving entities through `CoredeuxService`
- loading saved entities through `CoredeuxService`
- updating an entity through the framework path
- searching normal fields and JSONB metadata
- using PostgreSQL JSONB comparators such as `JSONB(TEXT)` and `JSONB(NUMERIC)`

Bootstrap can be disabled with:

```text
coredeux.demo.bootstrap.enabled=false
```

## OpenAPI Configuration

[DemoOpenApiConfiguration.java](../../examples/coredeux-demo/src/main/java/com/coredeux/demo/config/DemoOpenApiConfiguration.java)
adds basic OpenAPI metadata for Swagger UI.

This is intentionally small. Its purpose is to make the demo easy to explore
from the browser.

## How To Read The Demo

Use this order when studying the code:

1. Start with `coredeux-entities.yml`.
2. Open the CRUD controller and see how little application code is needed to
   call `CoredeuxService`.
3. Read the JDBC, MongoDB, Elasticsearch, and Redis entity examples to see how
   the same controller works across multiple data-access services.
4. Read the validator, hook, and audit handlers.
5. Read the workflow module to understand how new modules are added.
6. Read the import handlers and import controllers.
7. Read the export storage service and export controller.
8. Finish with `DemoDataRunner` to see data and JSONB search in action.

## What To Copy Into Your App

Copy the patterns, not necessarily the exact classes:

- entity definitions in YAML
- Spring bean handlers referenced by name
- validators for business rules
- hooks for lifecycle behavior
- audit handlers for operation capture
- custom modules for domain-specific framework behavior
- import value handlers for type conversion
- export storage services for artifact persistence
- generic controllers or agent tools when your application needs dynamic
  entity access

<!-- docs-nav-start -->
[Previous: Docker Demo Setup](docker-demo.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Adding A New Entity](add-new-entity.md)
<!-- docs-nav-end -->
