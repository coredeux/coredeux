# Coredeux Java Native Demo

This module shows the Coredeux demo running without Spring. It boots a plain Java
runtime, wires Coredeux services by hand, and exposes the same demo story through
an embedded HTTP server.

The native demo is intentionally compact, but it still shows the full shape of a
real Coredeux app:

- entity definitions loaded from YAML
- JPA, JDBC, MongoDB, Elasticsearch, and Redis data access services
- validators, hooks, audit handlers, and workflows
- import and export support
- Swagger UI and an OpenAPI document for the native HTTP surface
- a small HTTP server that exposes the demo over HTTP

## Bundled API assets

The native module ships with the same Postman-style collections and sample
files used by the Spring demo, adjusted for the native runtime:

- `postman/coredeux-java-native-demo.postman_collection.json`
- `postman/coredeux-java-native-demo-import.postman_collection.json`
- `src/main/resources/samples/import-products.import`
- `src/main/resources/samples/import-products.xlsx`
- `src/main/resources/samples/import-jdbc-inventory.import`
- `src/main/resources/samples/import-mongodb-audit-trails.import`
- `src/main/resources/samples/import-elasticsearch-catalog.import`
- `src/main/resources/samples/import-redis-sessions.import`

The older `src/main/resources/samples/postgres-customers.import` sample stays in
place for the lower-level Postgres walkthrough and the `/api/import/sample`
endpoint.

## Main entry point

The application starts here:

- `com.coredeux.examples.nativejava.CoredeuxNativeDemoApplication`

That class does only three things:

1. reads the native port from `COREDEUX_NATIVE_PORT`
2. creates the native runtime with `CoredeuxNativeRuntime.create()`
3. starts `CoredeuxNativeDemoServer.start(runtime, port)`

It also installs a shutdown hook so the HTTP server and runtime are closed
cleanly when the JVM exits.

## Important runtime classes

### `com.coredeux.examples.nativejava.CoredeuxNativeRuntime`

This is the heart of the native demo. It owns all the runtime wiring and keeps
the demo boot sequence in one place.

`CoredeuxNativeRuntime.create()` performs the startup flow:

1. loads `META-INF/coredeux.yml` through `CoredeuxPropertiesLoader`
2. reads `entities.config-location`
3. loads the entity definitions from `coredeux-entities.yml`
4. creates the Postgres JPA entity manager factory
5. creates the JDBC, MongoDB, Elasticsearch, and Redis data access services
6. registers all handlers and modules in `InMemoryCoredeuxComponentRegistry`
7. builds the Coredeux strategy and module service
8. creates the import and export services
9. starts the export worker scheduler when enabled

The runtime also exposes the important objects used by the server:

- `entityDefinitionRegistry()`
- `coredeuxService()`
- `coredeuxModuleService()`
- `coredeuxImportService()`
- `coredeuxExportService()`
- `reflectionHelperService()`

Helpful methods inside the runtime:

- `customerId(String prefix)` creates the generated IDs used by the demo
- `processPendingExportJobs()` lets the demo trigger export processing manually
- `close()` shuts down the worker and the JPA entity manager cleanly

### `com.coredeux.examples.nativejava.CoredeuxNativeDemoServer`

This is the embedded HTTP server. It uses `com.sun.net.httpserver.HttpServer`
and exposes the native demo over HTTP.

The server registers these routes:

- `/health`
- `/swagger-ui.html`
- `/v3/api-docs`
- `/api/entities/{entityName}`
- `/api/import`
- `/api/export`

The server is responsible for:

- mapping requests to Coredeux services
- reading and writing JSON
- resolving entity types from the Coredeux registry
- translating path parameters into entity identifiers
- exposing Swagger UI and the generated OpenAPI document for the HTTP surface

The server is started by `CoredeuxNativeDemoServer.start(runtime, port)`.
It also owns the HTTP executor and shuts it down in `close()`.

### `com.coredeux.examples.nativejava.NativeEntityResolver`

This helper keeps the HTTP layer small.

It resolves:

- entity definitions from the registry
- Java classes from full class names
- identifier field values
- identifier field types
- identifier assignment during create/update flows

The server uses it so that route handling stays focused on HTTP, not reflection.

## Package map

### `com.coredeux.examples.nativejava`

This package contains the native app bootstrap and server pieces:

- `CoredeuxNativeDemoApplication`
- `CoredeuxNativeRuntime`
- `CoredeuxNativeDemoServer`
- `NativeEntityResolver`

### `com.coredeux.examples.nativejava.postgres`

This package contains the smaller Postgres walkthrough programs:

- `PostgresCustomerMain`
- `PostgresCustomerImportMain`

These are lower-level examples that show the Postgres-native path without the
full HTTP server.

### `com.coredeux.examples.nativejava.module`

This package contains the native module handlers used in the demo:

- `CustomerNameValidator`
- `CustomerLifecycleHook`
- `CustomerAuditHandler`

These classes are registered manually in the native runtime component registry.

### `com.coredeux.demo.domain`

This package holds the shared demo model used by the native app:

- `Customer`
- `Product`
- `Role`
- `Address`
- `CustomerProfile`
- `CustomerOrder`
- `OrderItem`
- `Item`
- enums such as `CustomerStatus`, `ProductCategory`, `OrderStatus`, `RoleType`, `AddressType`
- `UriAttributeConverter`

These are the main JPA-backed demo entities and supporting value types.

### `com.coredeux.demo.domain.jdbc`

This package contains the JDBC example model:

- `JdbcInventoryItem`

### `com.coredeux.demo.domain.mongodb`

This package contains the MongoDB example model:

- `MongoAuditTrail`

### `com.coredeux.demo.domain.elasticsearch`

This package contains the Elasticsearch example model:

- `ElasticsearchCatalogEntry`

### `com.coredeux.demo.domain.redis`

This package contains the Redis example model:

- `RedisSessionSnapshot`

### `com.coredeux.demo.validation`

This package contains demo validation logic:

- `CustomerEmailValidator`

### `com.coredeux.demo.hooks`

This package contains demo hook logic:

- `DemoLifecycleHook`
- `ExportStorageCleanupHook`

### `com.coredeux.demo.audit`

This package contains demo audit logic:

- `DemoAuditHandler`

### `com.coredeux.demo.imports`

This package contains import handlers:

- `DemoUriImportHandler`
- `LegacyDateImportHandler`

### `com.coredeux.demo.export`

This package contains export handlers and export-side storage support:

- `DateFormatExportHandler`
- `ExportStorageRecord`

### `com.coredeux.demo.workflow`

This package contains the workflow example:

- `CoredeuxDemoWorkflowHandler`
- `DemoCustomerApprovalWorkflow`
- `WorkflowsModuleHandler`

## Native bootstrap files

### `src/main/resources/META-INF/coredeux.yml`

This is the runtime configuration file for the native demo. It provides:

- the entity-definition location
- connection settings for Postgres, MongoDB, Elasticsearch, and Redis
- import defaults
- export defaults
- export worker settings

### `src/main/resources/coredeux-entities.yml`

This is the single entity-definition source used by the native demo. It maps
each demo entity to the right storage service and module configuration.

### `src/main/resources/META-INF/persistence.xml`

This file defines the JPA persistence unit used for the Postgres-backed part of
the demo. It lists only the classes that Hibernate should manage.

## How the demo boots

The runtime boot sequence is:

1. load `META-INF/coredeux.yml`
2. load `coredeux-entities.yml`
3. create the JPA entity manager factory
4. create the JDBC, MongoDB, Elasticsearch, and Redis adapters
5. register validators, hooks, audit handlers, workflows, import handlers, and export handlers
6. build the Coredeux strategy and module service
7. start the embedded HTTP server

The HTTP server then uses the runtime to:

- create, read, update, delete, and search entities
- run validation, hooks, audit, and workflows
- validate and parse imports
- queue and process export jobs
- serve download artifacts from export storage
- expose Swagger UI and `/v3/api-docs`

## Running the demo

Run the demo test/build from the repository root:

```bash
mvn -pl examples/coredeux-java-native-demo -am test
```

To run the Dockerized native demo, use the compose file inside the module
folder:

```bash
docker compose -f examples/coredeux-java-native-demo/docker-compose.yml up --build
```

To debug the native demo in Docker, run the same compose command with a JDWP
agent on port `5005`. Start the stack in this mode, keep that terminal open,
and attach your debugger to `localhost:5005` from your IDE.

PowerShell:

```powershell
$env:JAVA_TOOL_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005'
docker compose -f examples/coredeux-java-native-demo/docker-compose.yml up --build
Remove-Item Env:JAVA_TOOL_OPTIONS
```

bash or zsh:

```bash
JAVA_TOOL_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005' docker compose -f examples/coredeux-java-native-demo/docker-compose.yml up --build
```

If you want to debug the app locally instead of inside Docker, build the jar
and run it with the same JDWP agent:

```powershell
mvn -pl examples/coredeux-java-native-demo -am -DskipTests package
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 -jar examples/coredeux-java-native-demo/target/coredeux-java-native-demo-0.1.0-SNAPSHOT.jar
```

In that local-debug case, you still attach to `localhost:5005`, but the app is
running directly from the jar instead of Docker.

## Environment variables

The native demo reads optional environment variables for the databases:

```bash
COREDEUX_NATIVE_PORT=8080
COREDEUX_POSTGRES_URL=jdbc:postgresql://localhost:5432/coredeux
COREDEUX_POSTGRES_USER=postgres
COREDEUX_POSTGRES_PASSWORD=root
COREDEUX_DEMO_MONGO_HOST=localhost
COREDEUX_DEMO_MONGO_PORT=27017
COREDEUX_DEMO_ELASTICSEARCH_HOST=localhost
COREDEUX_DEMO_ELASTICSEARCH_PORT=9200
COREDEUX_DEMO_ELASTICSEARCH_SCHEME=http
COREDEUX_DEMO_REDIS_HOST=localhost
COREDEUX_DEMO_REDIS_PORT=6379
```

## What to read next

If you want the framework story first, start with the docs homepage and the
Coredeux core reference.

If you want the demo mechanics first, start with the package map above and then
follow the runtime creation flow in `CoredeuxNativeRuntime.create()`.
