# Coredeux

Coredeux is a modular Java framework for building applications that need a
shared, governed way to handle common work.

It is not just a CRUD library. It helps you build software where data changes,
imports, exports, validation, hooks, audit, and automation all follow the same
style instead of being scattered across every service.

The goal is simple: application code should express intent, and Coredeux should
take care of the repeatable parts around that intent.

## What Coredeux Is For

Coredeux is useful when you want to:

- keep business operations consistent across an application
- reuse the same patterns for create, update, fetch, import, export, and
  automation flows
- avoid rewriting the same validation and hook logic in every service
- swap backend implementations without changing the way the application talks
  to the framework
- build toward agent-friendly capabilities that can be discovered and called
  through clear contracts

## What Coredeux Is Not

Coredeux is not a front-end application.

It is not a CMS or portal suite in the old "everything in one box" sense.

It is not trying to replace your domain model. Instead, it provides the
runtime around your model so the same capability can be executed in a
consistent way.

## Simple Mental Model

Think of Coredeux as:

`define the capability -> apply framework behavior -> run it through the right backend`

That means the framework can keep the public experience stable while the
implementation behind it changes by module or storage type.

Entity management is one part of that model, but not the whole story. Import,
export, and future agent-facing tools sit in the same direction.

Current lifecycle operations used across the framework are:

- `CREATE`
- `MODIFY`
- `UPSERT`
- `DELETE`
- `FETCH`

These lifecycle events are carried through the framework context so validators,
hooks, audit handlers, and future module types all see the same operation
model.

## Why It Exists

Most applications end up re-implementing the same patterns:

- load or update records
- check inputs
- run business-specific side effects
- import and export data
- keep behavior consistent across different storage choices

Coredeux makes those steps consistent and configurable instead of bespoke in
every service.

That becomes especially useful when the same capability needs to work across
different persistence technologies, or when import/export and agent-driven
actions should follow the same framework contract as normal application calls.

## Is Coredeux Relevant In The Age Of Claude And Codex?

Yes, because agents generate and operate workflows, while Coredeux hosts,
validates, persists, imports, exports, and governs them.

AI coding tools can produce application code quickly, but long-lived systems
still need consistency beyond a single prompt session. Teams need stable
conventions, repeatable module patterns, safe data access paths, import and
export contracts, and examples that can be understood, reused, and extended.

Coredeux is designed to be that agent-friendly application substrate for
Java/Spring systems:

- a framework for AI-assisted enterprise applications
- a reference architecture for CRUD, search, import, export, and workflow-heavy
  systems
- a stable backend foundation that agents such as Codex and Claude can extend
  predictably
- a demo platform that shows how new modules can be added without inventing the
  structure from scratch each time
- a practical framework for teams that still want control over the code they
  copy, run, test, and own

The name Coredeux reflects that direction: a foundational core for data,
services, workflow, and integration. In the agent era, Coredeux aims to be more
than another Java utility framework. It is infrastructure that makes
agent-assisted development easier to trust: clear docs, predictable module
conventions, working examples, clean tests, and room for AI-ready recipes such
as "add a new entity module using this pattern."

## Start Here

If you are new to Coredeux, these are the two fastest paths into the project:

- [First 10 Minutes With Coredeux](docs/guides/first-10-minutes.md): the quick,
  hands-on guide for running `coredeux-demo`, understanding the prerequisites,
  and getting the framework running locally.
- [Docker Demo Setup](docs/guides/docker-demo.md): the easiest way to run the
  demo stack locally with Docker.
- [Coredeux Demo Tour](docs/guides/demo-tour.md): a guided walk through the demo
  app so you can see what was customized and how to extend the framework.

## What Is Included

This repository includes the building blocks for that model:

- `coredeux-core`
  framework contracts and common runtime behavior
- `coredeux-core-jpa`
  JPA-based backend support, including:
  - `defaultCoredeuxJpaDataAccessService` for generic JPA/Criteria usage
  - `postgresCoredeuxJpaDataAccessService` for PostgreSQL JSONB/native-query scenarios
- `coredeux-core-jdbc`
  direct SQL / JDBC-backed support with `defaultCoredeuxJdbcDataAccessService`
- `coredeux-core-elasticsearch`
  Elasticsearch-backed support with `defaultCoredeuxElasticsearchDataAccessService`
- `coredeux-core-mongodb`
  MongoDB-backed backend support:
  - `defaultCoredeuxMongoDataAccessService` for MongoTemplate-based CRUD, search, and query usage
- `coredeux-core-redis`
  Redis-backed backend support:
  - `defaultCoredeuxRedisDataAccessService` for JSON-backed storage, CRUD, search, and query usage
  - storage-oriented, not a cache abstraction
- `coredeux-import`
  raw JSON import support
- `coredeux-import-parser`
  text and Excel import-file parsing
- `coredeux-export`
  export queueing, file generation, logs, and storage services
- `examples/coredeux-demo`
  Spring Boot demo application that shows the framework in action
  and includes examples for PostgreSQL, JDBC, MongoDB, Elasticsearch, and Redis
  through the same generic CRUD controller

## Future Direction

The roadmap includes MCP capabilities so agents can discover and call framework
operations through governed tool contracts. Coredeux is also moving toward
agent-aware implementations where agents become first-class framework citizens.
That means identity, intent, permissions, and traceability should travel with
the work instead of being bolted on later.

## Documentation Map

The main documentation entry point is [docs/README.md](docs/README.md).
The generated-site reading order is tracked in [docs/SUMMARY.md](docs/SUMMARY.md).

Useful starting points:

- [Architecture Overview](docs/architecture/overview.md)
- [Entity Definitions](docs/configuration/entity-definitions.md)
- [Module System](docs/features/modules.md)
- [Adding A New Entity](docs/guides/add-new-entity.md)
- [Core Module Reference](docs/modules/core/reference.md)
- [Core JPA Module Reference](docs/modules/core-jpa/reference.md)
- [Core Elasticsearch Module Reference](docs/modules/core-elasticsearch/reference.md)
- [Core MongoDB Module Reference](docs/modules/core-mongodb/reference.md)
- [Core Redis Module Reference](docs/modules/core-redis/reference.md)
- [Import Tutorial](docs/modules/import/guide.md)
- [Import File Tutorial](docs/modules/import-parser/guide.md)
- [Export Guide](docs/modules/export/guide.md)

## Build

Run all tests:

```powershell
mvn test
```

Run only core tests:

```powershell
mvn -pl modules/coredeux-core test
```

Run core JPA tests with dependencies:

```powershell
mvn -pl modules/coredeux-core-jpa -am test
```

Run core MongoDB tests with dependencies:

```powershell
mvn -pl modules/coredeux-core-mongodb -am test
```

Run core Redis tests with dependencies:

```powershell
mvn -pl modules/coredeux-core-redis -am test
```

Run core Elasticsearch tests with dependencies:

```powershell
mvn -pl modules/coredeux-core-elasticsearch -am test
```

Run the demo in Docker:

```powershell
docker compose -f examples/coredeux-demo/docker-compose.yml up --build
```

## Status

The framework is still evolving, but the core shape is already in place:

- consistent application-level workflows
- configurable extension points
- import and export support
- JPA, JDBC, Elasticsearch, MongoDB, Redis, and related backend adapters
- agent-oriented direction in the roadmap

The tutorials in `docs/guides/` are the best entry point if you want to see
the framework in action before reading the module references.

## License

Licensed under the Apache License, Version 2.0.
