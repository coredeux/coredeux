# coredeux-demo

`coredeux-demo` is a standalone Spring Boot application that demonstrates the
current Coredeux framework modules together:

- `coredeux-core`
- `coredeux-core-jpa`
- `coredeux-import`
- `coredeux-export`

The repository also includes JDBC, Elasticsearch, MongoDB, and Redis backend
modules. The Docker stack starts those services so the broader framework
examples have a ready local environment.

The demo also includes example entity classes for those backends:

- `com.coredeux.demo.domain.jdbc.JdbcInventoryItem`
- `com.coredeux.demo.domain.mongodb.MongoAuditTrail`
- `com.coredeux.demo.domain.elasticsearch.ElasticsearchCatalogEntry`
- `com.coredeux.demo.domain.redis.RedisSessionSnapshot`

For the full tutorial flow, start with
[First 10 Minutes With Coredeux](../../docs/guides/first-10-minutes.md).
For an explanation of the extension examples in this demo, read
[Coredeux Demo Tour](../../docs/guides/demo-tour.md).
For a containerized local setup, read
[Docker Demo Setup](../../docs/guides/docker-demo.md).

## Prerequisites

- For local host runs:
  - Java 17
  - Maven 3.9 or a compatible Maven 3.x version
  - PostgreSQL 14 or newer
- For Docker runs:
  - Docker Desktop on Windows or macOS, or Docker Engine plus Compose on Linux

Tests use Testcontainers, so Docker is also required when running demo tests.

## Docker Quick Start

The easiest way to run the demo is through the container stack:

```powershell
docker compose -f examples/coredeux-demo/docker-compose.yml up --build
```

That starts the demo app together with PostgreSQL, MongoDB, Redis, and
Elasticsearch.

Open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Stop the stack with:

```powershell
docker compose -f examples/coredeux-demo/docker-compose.yml down
```

## Database

Create a disposable PostgreSQL database:

```sql
CREATE DATABASE coredeux_oss;
```

Default connection:

```text
host: localhost
port: 5432
database: coredeux_oss
username: postgres
password: root
```

The demo uses Hibernate `ddl-auto: create-drop`, so tables are created at
startup and dropped when the app stops.

## Configuration

Database settings live in `src/main/resources/application.yml` and can be
overridden with environment variables:

```powershell
$env:COREDEUX_DEMO_DB_HOST = "localhost"
$env:COREDEUX_DEMO_DB_PORT = "5432"
$env:COREDEUX_DEMO_DB_NAME = "coredeux_oss"
$env:COREDEUX_DEMO_DB_USER = "postgres"
$env:COREDEUX_DEMO_DB_PASSWORD = "root"
```

Entity definitions live in:

```text
src/main/resources/coredeux-entities.yml
```

The demo uses `postgresCoredeuxJpaDataAccessService` for managed entities. Use
that strategy when exercising PostgreSQL JSONB/native-query behavior. Use
`defaultCoredeuxJpaDataAccessService` in your own app when generic JPA/Criteria
behavior is enough.

The Docker stack provides PostgreSQL automatically, so you only need the manual
database setup if you are running the app directly on your machine.

The new backend-specific example entities are wired through
`coredeux-entities.yml`, so the same `/api/entities/{entityName}` CRUD
controller can work with different storage services simply by changing the
fully qualified class name in the path.

## Run

From the repository root:

```powershell
mvn -pl examples/coredeux-demo -am spring-boot:run
```

Open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Use a different port if needed:

```powershell
mvn -pl examples/coredeux-demo -am spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

## What The Demo Shows

- PostgreSQL-backed `CoredeuxService` CRUD/search
- JSONB search support
- validators, lifecycle hooks, audit handlers, and export cleanup hooks
- a custom `workflows` module
- raw JSON import validation and execution
- text and Excel import-file validation and execution
- enums
- `@ManyToOne`
- `@OneToMany`
- `@ManyToMany`
- `@OneToOne`
- collection-valued attributes using `@ElementCollection`
- async export queueing, storage backends, status lookup, and download

## Useful Endpoints

CRUD:

- `POST /api/entities/{entityName}`
- `GET /api/entities/{entityName}/{id}`
- `GET /api/entities/{entityName}`
- `PUT /api/entities/{entityName}/{id}`
- `DELETE /api/entities/{entityName}/{id}`

Import:

- `POST /api/import/validate`
- `POST /api/import`
- `POST /api/import/file/validate`
- `POST /api/import/file`

Export:

- `POST /api/export`
- `GET /api/export/{uid}`
- `GET /api/export/{uid}/download`

The export flow uses configurable storage and log backends. By default the
demo stores exported files through `databaseCoredeuxExportStorageService`, and
the export log service is selected through
`coredeux.export.log.default-service`. The bundled options are
`defaultCoredeuxExportLogService` for file-backed logs and
`consoleCoredeuxExportLogService` for console-only logs.

The CRUD controller resolves entities by fully qualified class name, such as:

- `com.coredeux.demo.domain.Customer`
- `com.coredeux.demo.domain.Product`
- `com.coredeux.demo.domain.CustomerOrder`

## Postman Collections

Sample Postman collections and import files are included under `postman/`:

- [coredeux-demo.postman_collection.json](postman/coredeux-demo.postman_collection.json)
- [coredeux-demo-import.postman_collection.json](postman/coredeux-demo-import.postman_collection.json)
- [import-products.import](postman/samples/import-products.import)
- [import-products.xlsx](postman/samples/import-products.xlsx)

## Tests

Run demo tests:

```powershell
mvn -pl examples/coredeux-demo -am test
```

Run the full repository test suite:

```powershell
mvn test
```
