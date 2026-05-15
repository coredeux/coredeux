# Coredeux

Coredeux is a modular Java framework for enterprise applications that need a
shared, governed way to handle common work.

It is not just a CRUD library. It is meant to sit around the application so
the recurring parts of enterprise software stay consistent instead of being
scattered across every service.

The goal is simple: application code should express intent, and Coredeux
should take care of the repeatable parts around that intent.

## The Simple Mental Model

Think of Coredeux like this:

```text
define the capability -> apply framework behavior -> run it through the right backend
```

That is the shape the framework is trying to keep stable.

An application says what it wants to do. Coredeux takes care of the repeatable
parts around that intent.

## What Coredeux Is For

Coredeux is useful when you want to:

- keep business operations consistent across an application
- reuse the same patterns for create, update, fetch, import, export, and
  workflow-style flows
- avoid rewriting the same validation and hook logic in every service
- swap backend implementations without changing the way the application talks
  to the framework
- build toward agent-friendly capabilities that can be discovered and called
  through clear contracts

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

Yes. AI coding tools can produce application code quickly, but long-lived
systems still need consistency beyond a single prompt session. Teams need
stable conventions, repeatable module patterns, safe data access paths,
import/export contracts, and examples that can be understood, reused, and
extended.

Coredeux is designed to be that agent-friendly application substrate for
Java/Spring systems:

- a framework for AI-assisted enterprise applications
- a reference architecture for CRUD, search, import, export, and
  workflow-heavy systems
- a stable backend foundation that agents such as Codex and Claude can extend
  predictably
- a demo platform that shows how new modules can be added without inventing the
  structure from scratch each time
- a practical framework for teams that still want control over the code they
  copy, run, test, and own

The name Coredeux reflects that direction: a foundational core for data,
services, workflow, and integration. In the agent era, Coredeux aims to be
more than another Java utility framework. It is infrastructure that makes
agent-assisted development easier to trust: clear docs, predictable module
conventions, working examples, clean tests, and room for AI-ready recipes such
as "add a new entity module using this pattern."

## What Is Included

This repository includes the building blocks for that model:

- `modules/coredeux-core`
  core framework contracts and runtime behavior
- `modules/coredeux-import`
  raw JSON import support plus text and Excel import-file parsing
- `modules/coredeux-export`
  export queueing, file generation, logs, and storage services
- `modules/coredeux-core-*`
  native data-access implementations for JPA, JDBC, MongoDB, Elasticsearch,
  and Redis
- `modules/spring-boot-starters/*`
  Spring Boot starter modules for Spring-based hosts
- `examples/coredeux-spring-boot-demo`
  Spring Boot demo application that shows the framework in action
- `examples/coredeux-java-native-demo`
  native Java demo application that shows the same core stories without Spring
- `docs/`
  the hosted documentation site, including the numbered docs and demo tours

## What Coredeux Is Not

Coredeux is not:

- a front-end application
- a database
- only Spring Boot
- only plain Java
- a single storage implementation
- a finished platform with no more evolution

The demos are examples of how to consume the framework. They are not the
framework itself.

## Quick Links

- [Documentation Home](docs/README.md)
- [What Is Coredeux?](docs/01-what-is-coredeux.md)
- [Getting Started In 10 Minutes](docs/02-getting-started-in-10-minutes.md)
- [Tour Of The Demo](docs/03-tour-of-the-demo.md)
- [Coredeux Core Reference](docs/modules/coredeux-core/13-reference.md)
- [Coredeux Import Reference](docs/modules/coredeux-import/04-reference.md)
- [Coredeux Export Reference](docs/modules/coredeux-export/04-reference.md)

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

Run the Spring Boot demo in Docker:

```powershell
docker compose -f examples/coredeux-spring-boot-demo/docker-compose.yml up --build
```

Run the native demo:

```powershell
mvn -pl examples/coredeux-java-native-demo -am test
```

## Status

The framework is still evolving, but the core shape is already in place:

- consistent application-level workflows
- configurable extension points
- import and export support
- JPA, JDBC, Elasticsearch, MongoDB, Redis, and related backend adapters
- agent-oriented direction in the roadmap

The numbered docs in `docs/` are the best entry point if you want to see the
framework in action before reading the module references.

## License

The current Coredeux repository release is licensed under Apache License 2.0.

License terms may change for future major versions. Each released version is
governed by the license included with that version at the time of release.
Earlier released versions remain subject to the license under which they were
published.

See also:

- [NOTICE](NOTICE)
- [TRADEMARKS.md](TRADEMARKS.md)
