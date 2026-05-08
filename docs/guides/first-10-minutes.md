# First 10 Minutes With Coredeux

<!-- docs-nav-start -->
[Previous: Guides](README.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Docker Demo Setup](docker-demo.md)
<!-- docs-nav-end -->

This guide gets the `coredeux-demo` application running locally and shows the
fastest path through the framework: start the demo, open Swagger UI, create an
entity, validate an import, parse an import file, and queue an export.

If you prefer Docker, use the dedicated [Docker Demo Setup](docker-demo.md)
guide first. It starts the demo app together with PostgreSQL, MongoDB, Redis,
and Elasticsearch.

Use this as the first hands-on tutorial. The next guide, [Adding A New
Entity](add-new-entity.md), explains how to add your own managed entity after
the demo is running.

## What You Will Run

`examples/coredeux-demo` is a Spring Boot application that wires together:

- `coredeux-core`
- `coredeux-core-jpa`
- `coredeux-import`
- `coredeux-import-parser`
- `coredeux-export`

The demo uses PostgreSQL and the `postgresCoredeuxJpaDataAccessService`
strategy so JSONB-aware search behavior is exercised against the same database
type the demo is designed for.

The demo exposes:

- generic CRUD endpoints at `/api/entities/{entityName}`
- raw JSON import endpoints at `/api/import`
- text and Excel import-file endpoints at `/api/import/file`
- export queue/status/download endpoints at `/api/export`
- Swagger UI at `/swagger-ui.html`

## Prerequisites

Install:

- Java 17
- Maven 3.9 or a compatible Maven 3.x version
- PostgreSQL 14 or newer

The project is built with Java 17 and Spring Boot 3.3.0. PostgreSQL is required
for running the demo application because the demo is intentionally configured
with the PostgreSQL data access strategy.

If you use Docker instead of local services, you only need Docker Desktop or
the Docker Engine plus Docker Compose. The container stack provides the
database services for you.

Tests use Testcontainers for PostgreSQL, but the running demo expects a real
PostgreSQL database reachable from the application.

## 1. Create The Demo Database

Create a PostgreSQL database for the demo.

Default database name:

```text
coredeux_oss
```

Using `psql`:

```sql
CREATE DATABASE coredeux_oss;
```

The default local connection expected by the demo is:

```text
host: localhost
port: 5432
database: coredeux_oss
username: postgres
password: root
```

You can either create/use a matching local PostgreSQL user, or override the
connection values with environment variables in the next step.

Important: the demo uses Hibernate `ddl-auto: create-drop`, so tables are
created when the application starts and dropped when it stops. Use a disposable
database.

## 2. Configure Database Credentials

The demo reads database settings from
`examples/coredeux-demo/src/main/resources/application.yml`.

The relevant configuration is:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${COREDEUX_DEMO_DB_HOST:localhost}:${COREDEUX_DEMO_DB_PORT:5432}/${COREDEUX_DEMO_DB_NAME:coredeux_oss}
    username: ${COREDEUX_DEMO_DB_USER:postgres}
    password: ${COREDEUX_DEMO_DB_PASSWORD:root}
```

Prefer environment variables over editing `application.yml`:

```powershell
$env:COREDEUX_DEMO_DB_HOST = "localhost"
$env:COREDEUX_DEMO_DB_PORT = "5432"
$env:COREDEUX_DEMO_DB_NAME = "coredeux_oss"
$env:COREDEUX_DEMO_DB_USER = "postgres"
$env:COREDEUX_DEMO_DB_PASSWORD = "root"
```

For Bash:

```bash
export COREDEUX_DEMO_DB_HOST=localhost
export COREDEUX_DEMO_DB_PORT=5432
export COREDEUX_DEMO_DB_NAME=coredeux_oss
export COREDEUX_DEMO_DB_USER=postgres
export COREDEUX_DEMO_DB_PASSWORD=root
```

## 3. Understand The Data Access Strategy

The demo entity definitions live in:

```text
examples/coredeux-demo/src/main/resources/coredeux-entities.yml
```

Demo entities use:

```yaml
storage:
  data-access-service: postgresCoredeuxJpaDataAccessService
```

That means Coredeux resolves persistence through the PostgreSQL JPA adapter.
This is the right strategy for the demo because it includes JSONB-aware query
support.

For your own application:

- use `postgresCoredeuxJpaDataAccessService` for PostgreSQL JSONB/native-query
  scenarios
- use `defaultCoredeuxJpaDataAccessService` for generic JPA/Criteria scenarios

## 4. Start The Demo

From the repository root:

```powershell
mvn -pl examples/coredeux-demo -am spring-boot:run
```

The `-am` flag builds required Coredeux modules first.

When startup completes, open:

```text
http://localhost:8080/swagger-ui.html
```

The OpenAPI document is available at:

```text
http://localhost:8080/v3/api-docs
```

The application enables scheduling so the export worker can process queued
export jobs in the background.

## 5. Check The Managed Entities

The demo registers entities in `coredeux-entities.yml`, including:

- `com.coredeux.demo.domain.Customer`
- `com.coredeux.demo.domain.Product`
- `com.coredeux.demo.domain.CustomerOrder`
- `com.coredeux.demo.domain.Role`
- `com.coredeux.demo.domain.Address`
- `com.coredeux.demo.domain.CustomerProfile`
- `com.coredeux.demo.domain.OrderItem`
- `com.coredeux.demo.export.ExportStorageRecord`

Each definition tells Coredeux:

- the Java class
- the entity name
- the identifier field
- the data access service strategy
- optional modules such as validators, hooks, audit, or workflows

The demo seeds a small PostgreSQL dataset at startup through `DemoDataRunner`.
Additional import-file samples live under `examples/coredeux-demo/samples`.

## 6. Try CRUD Through Swagger

In Swagger UI, open the `Coredeux Demo CRUD` group.

List products:

```text
GET /api/entities/com.coredeux.demo.domain.Product
```

Create a product:

```text
POST /api/entities/com.coredeux.demo.domain.Product
```

Example body:

```json
{
  "sku": "DEMO-PRODUCT-100",
  "name": "Demo Product 100",
  "price": 49.95,
  "documentationUrl": "https://docs.coredeux.dev/demo/products/demo-product-100",
  "category": "SOFTWARE",
  "active": true
}
```

Update and delete use the same fully qualified entity class name:

```text
PUT /api/entities/com.coredeux.demo.domain.Product/{id}
DELETE /api/entities/com.coredeux.demo.domain.Product/{id}
```

From the outside this looks like generic CRUD. Inside Coredeux, the request goes
through `CoredeuxService`, entity definition resolution, lifecycle context,
modules, and the configured data access strategy.

## 7. Validate A Raw JSON Import

Open the `Coredeux Demo Import` group.

If you want the import language behind this walkthrough, jump straight to
[Coredeux Raw JSON Import Tutorial](../modules/import/guide.md).

Validate only:

```text
POST /api/import/validate
```

Execute:

```text
POST /api/import
```

Example request:

```json
{
  "options": {
    "passes": 1,
    "failFast": false,
    "validateOnly": false
  },
  "statements": [
    {
      "operation": "UPSERT",
      "entity": "com.coredeux.demo.domain.Product",
      "columns": [
        { "name": "sku", "unique": true },
        { "name": "name" },
        { "name": "price" },
        { "name": "documentationUrl", "handler": "demoUriImportHandler" },
        { "name": "category" },
        { "name": "active", "defaultValue": "true" }
      ],
      "rows": [
        {
          "values": {
            "sku": "IMPORT-PRODUCT-100",
            "name": "Imported Product 100",
            "price": "59.95",
            "documentationUrl": "https://docs.coredeux.dev/demo/products/import-product-100",
            "category": "SOFTWARE"
          }
        }
      ]
    }
  ]
}
```

The import module validates the request, resolves existing entities using the
`unique` column, converts values, and then uses the same entity lifecycle path
as normal Coredeux service operations.

## 8. Validate A Text Or Excel Import File

Open the `Coredeux Demo File Import` group.

If you want the file syntax behind this walkthrough, jump straight to
[Coredeux Import File Tutorial](../modules/import-parser/guide.md).

Validate only:

```text
POST /api/import/file/validate
```

Execute:

```text
POST /api/import/file
```

Sample files are included in:

```text
examples/coredeux-demo/samples/
```

Available samples:

- `import-products.import`
- `import-products.xlsx`
- `import-jdbc-inventory.import`
- `import-mongodb-audit-trails.import`
- `import-elasticsearch-catalog.import`
- `import-redis-sessions.import`

Text import files compile into the same `ImportRequest` model:

```text
OPTIONS(passes=1,failFast=false)

&Product=com.coredeux.demo.domain.Product

UPSERT &Product | sku(unique=true) | name             | price | documentationUrl(handler=demoUriImportHandler)      | category | active(default=true)
                | FILE-PRODUCT-100 | File Product 100 | 69.95 | https://docs.coredeux.dev/demo/products/file-product | SOFTWARE |
```

For Excel uploads, use the optional `sheetName` parameter only when you want to
parse a named sheet. If it is omitted, the first workbook sheet is used.

## 9. Queue An Export

Open the `Coredeux Demo Export` group.

Queue an export:

```text
POST /api/export
```

Example request:

```json
{
  "entity": "com.coredeux.demo.domain.Product",
  "fieldList": [
    { "path": "sku" },
    { "path": "name" },
    { "path": "price" },
    { "path": "category" },
    { "path": "active" }
  ],
  "options": {
    "format": "XLSX",
    "includeHeader": true,
    "fileName": "products.xlsx",
    "storageService": "databaseCoredeuxExportStorageService"
  }
}
```

Read status:

```text
GET /api/export/{uid}
```

Download when complete:

```text
GET /api/export/{uid}/download
```

The demo uses `databaseCoredeuxExportStorageService` as the default export
storage backend. Filesystem storage is also available in the export module, and
its base directory can be configured with:

```text
COREDEUX_EXPORT_FILESYSTEM_BASE_DIRECTORY
```

Export logging is configurable too. The demo selects
`defaultCoredeuxExportLogService` by default, which writes logs to a file, but
you can switch to `consoleCoredeuxExportLogService` through
`coredeux.export.log.default-service` when you want the log output to stay in
the application console.

If you want the export module details behind this walkthrough, see
[Coredeux Export Guide](../modules/export/guide.md).

## 10. Run Tests

Run the demo tests:

```powershell
mvn -pl examples/coredeux-demo -am test
```

Run the full build:

```powershell
mvn test
```

Demo tests use Testcontainers for PostgreSQL. That means Docker must be running
for tests that need a PostgreSQL container.

## Troubleshooting

If the application cannot connect to PostgreSQL, check:

- PostgreSQL is running
- the `coredeux_oss` database exists
- `COREDEUX_DEMO_DB_*` values match your local database
- port `5432` is reachable

If port `8080` is already in use, pass a different server port:

```powershell
mvn -pl examples/coredeux-demo -am spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

If import validation fails, read the response logs. They identify statement,
row, field, and resolution issues where possible.

If export stays in `NEW` or `IN_PROGRESS`, make sure the application is still
running. The scheduled export worker is part of the demo application process.

## What To Read Next

- [Coredeux Demo Tour](demo-tour.md)
- [Adding A New Entity](add-new-entity.md)
- [Entity Definitions](../configuration/entity-definitions.md)
- [Raw JSON Import Tutorial](../modules/import/guide.md)
- [Import File Tutorial](../modules/import-parser/guide.md)
- [Export Guide](../modules/export/guide.md)

<!-- docs-nav-start -->
[Previous: Guides](README.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Docker Demo Setup](docker-demo.md)
<!-- docs-nav-end -->
