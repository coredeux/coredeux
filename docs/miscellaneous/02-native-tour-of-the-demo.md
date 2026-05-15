# Tour Of The Native Demo

<!-- docs-nav-start -->
[Previous: Native Getting Started In 10 Minutes](01-native-getting-started-in-10-minutes.md) | [Documentation Home](../README.md) | [Next: Import Into An Existing Spring Boot App](03-import-into-an-existing-spring-boot-app.md)
<!-- docs-nav-end -->

The native demo is the plain Java version of the Coredeux story.

It is compact, Docker-friendly, and intentionally close to the Spring Boot demo
in what it demonstrates. The difference is the host layer: no Spring container,
no auto-configuration, and no Spring MVC controller layer in the middle.

If you are a developer, this page shows where the native pieces live and what
each part is responsible for. If you are an AI agent reading the repository,
this is the map of the runtime surface you are expected to work with.

If you want to compare it with the Spring Boot version of the demo, see:

- [Getting Started In 10 Minutes](../02-getting-started-in-10-minutes.md)
- [Tour Of The Demo](../03-tour-of-the-demo.md)
- [Import Into An Existing Spring Boot App](03-import-into-an-existing-spring-boot-app.md)

## The Point Of The Demo

The demo exists to show that the framework can stay consistent while the
application is hosted entirely in plain Java:

- PostgreSQL-backed CRUD
- direct HTTP endpoints
- validation
- hooks
- audit
- import
- export
- a custom workflow module
- sample startup and sample import data

The key idea is that the demo does not invent a new pattern for each concern.
It keeps one Coredeux shape and wires it into a small native host.

## Where The Demo Lives

The demo application is under:

```text
examples/coredeux-java-native-demo
```

The important parts are:

- `src/main/java/com/coredeux/demo/CoredeuxDemoApplication.java`
  - native application entrypoint
- `src/main/java/com/coredeux/examples/nativejava/CoredeuxNativeRuntime.java`
  - the runtime composition root
- `src/main/java/com/coredeux/examples/nativejava/CoredeuxNativeDemoServer.java`
  - the embedded HTTP server
- `src/main/resources/META-INF/coredeux.yml`
  - Coredeux runtime properties
- `src/main/resources/META-INF/persistence.xml`
  - JPA persistence bootstrap
- `src/main/resources/coredeux-entities.yml`
  - managed entity definitions
- `src/main/resources/coredeux-postgres-entities.yml`
  - PostgreSQL-specific entity definitions
- `src/main/resources/samples`
  - sample import files used by the native import flow
- `src/main/java/com/coredeux/demo/domain`
  - the domain model used by the demo
- `src/main/java/com/coredeux/demo/validation`
  - validation examples
- `src/main/java/com/coredeux/demo/hooks`
  - lifecycle hook examples
- `src/main/java/com/coredeux/demo/audit`
  - audit handler example
- `src/main/java/com/coredeux/demo/imports`
  - custom import value handlers
- `src/main/java/com/coredeux/demo/export`
  - custom export value handlers and storage behavior
- `src/main/java/com/coredeux/demo/workflow`
  - custom module example
- `src/main/java/com/coredeux/demo/bootstrap`
  - startup data and search examples

## The Story In The Native Demo

The easiest way to understand the native demo is to read it as a stack of
layers.

### 1. The configuration layer

The native runtime starts from `META-INF/coredeux.yml`.

That file tells Coredeux things like:

- where to load the entity definitions from
- what the default import parser should be
- what the default export format should be
- how export storage and export workers should behave

This is the first important pattern in the native demo: framework settings live
outside the host code, just like JPA uses `persistence.xml`.

### 2. The runtime composition layer

`CoredeuxNativeRuntime` wires the whole application together manually.

It builds:

- the entity registry
- the reflection helper
- the data access services
- the module handlers
- the import service
- the export service
- the export worker

That is the native equivalent of a Spring application context.

### 3. The HTTP layer

`CoredeuxNativeDemoServer` exposes the application surface through a small
embedded `HttpServer`.

The main routes are:

- `/health`
- `/swagger-ui.html`
- `/v3/api-docs`
- `/api/entities/{entityName}`
- `/api/import`
- `/api/export`

This is where the demo feels like a real application instead of a library
example.

### 4. The module layer

The native demo still shows the framework modules working around the lifecycle:

- validators for business rules
- hooks for lifecycle behavior
- audit handlers for operation tracking
- workflow-style custom modules

The modules are the same idea you see in the Spring Boot demo. The host layer
is different, but the Coredeux behavior is the same.

### 5. The data and sample layer

`PostgresCustomerMain` and `PostgresCustomerImportMain` show that the runtime
can also be used without the HTTP server.

The sample import file under `src/main/resources/samples` gives the import flow
something real to process immediately.

## What To Notice First

When you read the native demo, start here:

1. `META-INF/coredeux.yml`
2. `META-INF/persistence.xml`
3. `CoredeuxNativeRuntime.java`
4. `CoredeuxNativeDemoServer.java`
5. `CoredeuxDemoApplication.java`
6. the validator, hook, and audit handlers
7. the workflow module
8. the import and export handlers
9. the sample import file

That order shows the framework from configuration to runtime to HTTP surface.

## Why The Native Demo Matters

The native demo proves a practical point:

Coredeux is not tied to Spring Boot. The same framework ideas can be hosted in a
plain Java application while still keeping the behavior consistent.

That matters for teams that want a lighter runtime, and it matters for agents
because the application surface stays explicit and easy to reason about.

## What To Copy Into Your App

You usually do not copy the demo as-is. You copy the patterns:

- runtime properties in `META-INF/coredeux.yml`
- entity definitions in YAML
- manual runtime composition
- an embedded HTTP server if you need one
- validators
- hooks
- audit handlers
- custom modules
- import value handlers
- export value handlers
- export storage services

Those are the reusable ideas the native demo is showing.

## What To Read Next

If you want the broader product story, continue with:

- [What Is Coredeux?](../01-what-is-coredeux.md)
- [Getting Started In 10 Minutes](../02-getting-started-in-10-minutes.md)
- [Overview](../overview/README.md)

<!-- docs-nav-start -->
[Previous: Native Getting Started In 10 Minutes](01-native-getting-started-in-10-minutes.md) | [Documentation Home](../README.md) | [Next: Platform Documentation](../platform/README.md)
<!-- docs-nav-end -->
