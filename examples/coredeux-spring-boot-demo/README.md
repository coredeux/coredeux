# Coredeux Spring Boot Demo

This module shows Coredeux running inside a Spring Boot application. It is the
Spring-hosted companion to the native demo and uses Spring Boot for the web
layer, bean wiring, scheduling, repository support, and startup lifecycle.

The demo shows the full Coredeux story through Spring Boot:

- entity definitions loaded from YAML
- Spring-managed JPA, JDBC, MongoDB, Elasticsearch, and Redis integration
- validators, hooks, audit handlers, and workflows
- import and export flows
- an OpenAPI / Swagger surface
- bootstrap data loading for a richer out-of-the-box demo

## Main entry point

The application starts here:

- `com.coredeux.demo.CoredeuxDemoApplication`

That class is the Spring Boot bootstrapper. It:

1. starts the Spring application context
2. enables scheduling
3. scans the `com.coredeux` package tree
4. lets Spring Boot and the Coredeux starters wire the runtime

## Important Spring configuration classes

### `com.coredeux.demo.config.DemoJpaConfiguration`

This class provides the named JPA data-access bean used by the demo entities:

- `postgresCoredeuxJpaDataAccessService`

Spring Boot creates the bean, and the Coredeux entity definitions refer to that
bean name in `coredeux-entities.yml`.

### `com.coredeux.demo.bootstrap.DemoDataRunner`

This class seeds the sample data when the app starts.

It is a `CommandLineRunner` and runs automatically when Spring Boot finishes
bootstrapping. Its `run(String... args)` method creates:

- roles
- products
- customers
- addresses
- customer profiles
- customer orders
- order items

It also exercises JSONB search so the demo shows richer Coredeux and JPA
behavior instead of a blank database.

The runner is gated by:

- `coredeux.demo.bootstrap.enabled`

### `com.coredeux.demo.config.DemoOpenApiConfiguration`

This class configures the OpenAPI / Swagger surface used by the demo.

## Important web classes

### `com.coredeux.demo.web.CoredeuxCrudController`

This is the generic CRUD and search controller. It exposes the main entity API
used by the demo.

### `com.coredeux.demo.web.CoredeuxImportController`

This controller handles JSON import requests.

### `com.coredeux.demo.web.CoredeuxImportFileController`

This controller handles file-based imports, including the text and Excel sample
files.

### `com.coredeux.demo.web.CoredeuxExportController`

This controller exposes the export API:

- create export request
- list export jobs
- download exported artifacts

### `com.coredeux.demo.web.CoredeuxDemoExceptionHandler`

This centralizes API error translation for the demo web layer.

### `com.coredeux.demo.web.DemoEntityResolver`

This helper keeps the controllers small.

It resolves:

- entity definitions from the registry
- Java classes from entity names
- identifier values from loaded objects
- identifier field types
- identifier assignment during create/update flows

## Package map

### `com.coredeux.demo`

This package contains the Spring Boot bootstrapper:

- `CoredeuxDemoApplication`

### `com.coredeux.demo.config`

This package contains Spring configuration used by the demo:

- `DemoJpaConfiguration`
- `DemoOpenApiConfiguration`

### `com.coredeux.demo.bootstrap`

This package contains startup data loading:

- `DemoDataRunner`

### `com.coredeux.demo.web`

This package contains the API layer:

- `CoredeuxCrudController`
- `CoredeuxImportController`
- `CoredeuxImportFileController`
- `CoredeuxExportController`
- `CoredeuxDemoExceptionHandler`
- `DemoEntityResolver`

### `com.coredeux.demo.domain`

This package holds the main demo model:

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

### `com.coredeux.demo.domain.jdbc`

JDBC example model:

- `JdbcInventoryItem`

### `com.coredeux.demo.domain.mongodb`

MongoDB example model:

- `MongoAuditTrail`

### `com.coredeux.demo.domain.elasticsearch`

Elasticsearch example model:

- `ElasticsearchCatalogEntry`

### `com.coredeux.demo.domain.redis`

Redis example model:

- `RedisSessionSnapshot`

### `com.coredeux.demo.validation`

Validation logic:

- `CustomerEmailValidator`

### `com.coredeux.demo.hooks`

Hook logic:

- `DemoLifecycleHook`
- `ExportStorageCleanupHook`

### `com.coredeux.demo.audit`

Audit logic:

- `DemoAuditHandler`

### `com.coredeux.demo.imports`

Import handlers:

- `DemoUriImportHandler`
- `LegacyDateImportHandler`

### `com.coredeux.demo.export`

Export helpers and storage support:

- `DateFormatExportHandler`
- `ExportStorageRecord`
- `ExportStorageRecordRepository`
- `DatabaseCoredeuxExportStorageService`

### `com.coredeux.demo.workflow`

Workflow example:

- `CoredeuxDemoWorkflowHandler`
- `DemoCustomerApprovalWorkflow`
- `WorkflowsModuleHandler`

## Demo bootstrap files

### `src/main/resources/application.yml`

This is the Spring Boot configuration file. It defines:

- Spring datasource settings
- JPA settings
- MongoDB, Elasticsearch, and Redis settings
- Coredeux settings such as entity config location, import defaults, export defaults, and worker settings

### `src/main/resources/coredeux-entities.yml`

This is the Coredeux entity-definition file. It maps each entity to:

- its identifier
- its storage adapter
- its module configuration

It is the key file that shows how Coredeux turns business entities into a
governed runtime model.

## How the demo boots

The startup flow is:

1. Spring Boot starts `CoredeuxDemoApplication`
2. Spring Boot scans `com.coredeux`
3. Spring creates the JPA bean in `DemoJpaConfiguration`
4. Coredeux starters adapt the Spring-managed infrastructure into Coredeux data-access services
5. Spring creates controllers, validators, hooks, and workflow components
6. `DemoDataRunner` seeds the sample data
7. the web controllers expose CRUD, import, export, and workflow examples

## What the demo shows

The Spring Boot demo is the hosted application story. It shows how Coredeux
fits into a normal Spring Boot app while still keeping the framework behavior
consistent across:

- CRUD
- validation
- hooks
- audit
- import
- export
- workflow execution
- multiple storage backends

## Running the demo

Run the demo test/build from the repository root:

```bash
mvn -pl examples/coredeux-spring-boot-demo -am test
```

To run the Dockerized Spring Boot demo:

```bash
docker compose -f examples/coredeux-spring-boot-demo/docker-compose.yml up --build
```

To debug the Spring Boot demo in Docker, run the same compose command with a
JDWP agent on port `5005`. Start the stack in this mode, keep that terminal
open, and attach your debugger to `localhost:5005` from your IDE.

PowerShell:

```powershell
$env:JAVA_TOOL_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005'
docker compose -f examples/coredeux-spring-boot-demo/docker-compose.yml up --build
Remove-Item Env:JAVA_TOOL_OPTIONS
```

bash or zsh:

```bash
JAVA_TOOL_OPTIONS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005' docker compose -f examples/coredeux-spring-boot-demo/docker-compose.yml up --build
```

If you want to debug the app locally instead of inside Docker, run:

```powershell
mvn -pl examples/coredeux-spring-boot-demo -am spring-boot:run "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
```

In that local-debug case, you still attach to `localhost:5005`, but the app is
running from Maven instead of Docker.

## Environment variables

The Spring Boot demo reads optional environment variables for the backing
databases and services:

```bash
COREDEUX_DEMO_MONGO_HOST=localhost
COREDEUX_DEMO_MONGO_PORT=27017
COREDEUX_DEMO_MONGO_DB=coredeux_oss
COREDEUX_DEMO_ELASTICSEARCH_HOST=localhost
COREDEUX_DEMO_ELASTICSEARCH_PORT=9200
COREDEUX_DEMO_ELASTICSEARCH_SCHEME=http
COREDEUX_DEMO_REDIS_HOST=localhost
COREDEUX_DEMO_REDIS_PORT=6379
```

## What to read next

If you want the platform story, read the core docs and the Spring Boot starter
references.

If you want the demo mechanics, start with `CoredeuxDemoApplication`, then
follow `DemoJpaConfiguration`, `DemoDataRunner`, and the controllers.
