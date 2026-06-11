# Coredeux

<!-- docs-nav-start -->
[Documentation Home](/) | [Next: Getting Started In 10 Minutes](/02-getting-started-in-10-minutes)
<!-- docs-nav-end -->

Coredeux standardizes the repetitive infrastructure of enterprise Java
applications and keeps it usable across both Spring Boot and native hosts.

It is a framework for the parts of an application that keep getting rebuilt:
validation, hooks, audit, import, export, workflow-style extensions, and
storage routing.

It is not just a CRUD library. It is meant to sit around the application so the
common framework behavior stays consistent instead of being scattered across
every service.

## The One-Line Identity

Coredeux is an enterprise application runtime for governed, contract-driven
behavior.

## The Simple Mental Model

Think of Coredeux like this:

```text
define the capability -> apply framework behavior -> run it through the right backend
```

That is the shape the framework is trying to keep stable.

An application says what it wants to do. Coredeux takes care of the repeatable
parts around that intent.

```mermaid
flowchart LR
  A[Application intent] --> B[Coredeux service]
  B --> C[Lifecycle context]
  C --> D[Validators]
  C --> E[Hooks]
  C --> F[Audit]
  C --> G[Import / Export]
  C --> H[Data access adapter]
  H --> I[(SQL / MongoDB / Elasticsearch / Redis)]
```

## Why It Exists

Enterprise systems keep rebuilding the same kinds of behavior:

- load and update records
- validate input
- apply hooks and side effects
- import and export data
- keep behavior consistent across different storage choices
- make the application easier to extend without rewriting the same patterns

Coredeux exists to make those pieces framework-owned instead of copied into
every new service.

That matters now because most Java enterprise apps still run in the Spring
Boot world, and in the future those apps will also need clean integration with
AI agents and MCP-style tools.

## Why Coredeux Is Relevant Now

Coredeux has a dual role:

- it provides the building blocks for enterprise applications
- it can embed into existing Java and Spring applications without forcing a
  rewrite

That means it can fit into the world we already have while preparing for the
world that is coming.

Future enterprise apps will not just be built by humans. They will also need to
work with agents, and those agents will need a framework they can understand
and use. Coredeux is trying to be that framework.

```mermaid
flowchart LR
  Agent[Claude / Codex / AI agent] -->|writes or extends| App[Enterprise app]
  App -->|uses| Core[Coredeux contracts]
  Core -->|governs| Ops[CRUD / import / export / workflow]
  Ops --> Backend[(Database / file / search / queue)]
```

## What The Repository Shows Today

This repository contains the current building blocks for that direction:

- core framework contracts and lifecycle context
- entity definition loading and registry support
- validator, hook, and audit contracts
- dynamic runtime DRL logic with native and Spring Boot entry points
- JPA, JDBC, MongoDB, Elasticsearch, and Redis data-access implementations
- import and export support
- Spring Boot starter modules for Spring-based hosts
- a native Java demo host for plain Java usage
- a Spring Boot demo host for Spring-based usage

These pieces are useful on their own, but they are also part of a larger story:
Coredeux is shaping the common mechanics so enterprise apps stay predictable as
they grow.

```mermaid
flowchart TB
  subgraph Hosts
    SB[Spring Boot demo]
    NV[Native demo]
    ST[Spring Boot starters]
  end

  subgraph Framework
    C[coredeux-core]
    I[coredeux-import]
    E[coredeux-export]
  end

  subgraph Backends
    JPA[JPA]
    JDBC[JDBC]
    MONGO[MongoDB]
    ES[Elasticsearch]
    REDIS[Redis]
  end

  Hosts --> Framework
  Framework --> Backends
```

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

## Why This Matters

Teams usually need more than a working endpoint. They need a consistent way to
keep business operations, backend selection, import/export flows, and future
integration paths aligned over time.

That is the value Coredeux is aiming for: keeping the common mechanics stable
enough that both humans and agents can work with them safely.

## 01. Start Here

- [Getting Started In 10 Minutes](/02-getting-started-in-10-minutes)
- [Tour Of The Demo](/03-tour-of-the-demo)

## 02. Product Direction

- [Overview](/overview/)
- [Vision](/overview/vision)
- [Roadmap](/overview/roadmap)

## 03. Coredeux Core

- [Coredeux Core](/modules/coredeux-core/)
- [Overview](/modules/coredeux-core/01-overview)
- [Lifecycle Model](/modules/coredeux-core/02-lifecycle)
- [Entity Definitions](/modules/coredeux-core/03-entity-definitions)
- [External Entity Definition Sources](/modules/coredeux-core/04-external-entity-definition-source)
- [Module System](/modules/coredeux-core/05-modules)
- [Add Or Choose A Data Access Service](/modules/coredeux-core/06-add-data-access-service)
- [Available Data Access Implementations](/modules/coredeux-core/07-available-data-access-implementations)
- [Add a Hook](/modules/coredeux-core/08-add-hook)
- [Add a Validator](/modules/coredeux-core/09-add-validator)
- [Add Audit](/modules/coredeux-core/10-add-audit)
- [Adding a New Entity](/modules/coredeux-core/11-add-new-entity)
- [Adding a Core Module](/modules/coredeux-core/12-add-core-module)
- [Reference](/modules/coredeux-core/13-reference)
- [Core JPA Reference](/modules/coredeux-core/14-core-jpa-reference)
- [Core JDBC Reference](/modules/coredeux-core/15-core-jdbc-reference)
- [Core Elasticsearch Reference](/modules/coredeux-core/16-core-elasticsearch-reference)
- [Core MongoDB Reference](/modules/coredeux-core/17-core-mongodb-reference)
- [Core Redis Reference](/modules/coredeux-core/18-core-redis-reference)
- [Property Resolution Order](/modules/coredeux-core/19-property-resolution-order)
- [Value Handlers](/modules/coredeux-core/20-value-handlers)

## 04. Import And Export

- [Coredeux Import](/modules/coredeux-import/01-overview)
- [Coredeux Export](/modules/coredeux-export/01-overview)

## 05. Rules And Runtime Logic

- [Coredeux DRL](/modules/coredeux-drl/)
- [DRL Overview](/modules/coredeux-drl/01-overview)
- [Native Runtime](/modules/coredeux-drl/02-native-runtime)
- [Spring Boot Starter](/modules/coredeux-drl/03-spring-boot-starter)
- [DRL Reference](/modules/coredeux-drl/04-reference)
- [Data Access With DRL](/modules/coredeux-drl/05-data-access)
- [Validators With DRL](/modules/coredeux-drl/06-validators)
- [Hooks With DRL](/modules/coredeux-drl/07-hooks)
- [Audit With DRL](/modules/coredeux-drl/08-audit)
- [Custom Handlers](/modules/coredeux-drl/09-custom-handlers)
- [Import Value Handlers](/modules/coredeux-drl/10-import-value-handlers)
- [Export Value Handlers](/modules/coredeux-drl/11-export-value-handlers)

## 06. Tools

- [Coredeux DRL DevTools](/modules/coredeux-drl-devtools/)

## 07. Native Demo Companion

- [Native Getting Started In 10 Minutes](/miscellaneous/01-native-getting-started-in-10-minutes)
- [Native Tour Of The Demo](/miscellaneous/02-native-tour-of-the-demo)

## 08. Spring Boot Import Integration

- [Import Into An Existing Spring Boot App](/miscellaneous/03-import-into-an-existing-spring-boot-app)

## 09. Platform And Project

- [Platform Documentation](/platform/)
- [Project](/project/)
- [Governance Model](/project/governance)
- [Release Process](/project/release-process)
- [Versioning And Branching](/project/versioning)
- [Security Policy](/project/security)
- [Third-Party Notices](/project/third-party-notices)

Use the core pages when you want the framework model, the module pages when
you want concrete behavior, and the project pages when you need governance or
release context.

<!-- docs-nav-start -->
[Documentation Home](/) | [Next: Getting Started In 10 Minutes](/02-getting-started-in-10-minutes)
<!-- docs-nav-end -->
