# What Is Coredeux?

<!-- docs-nav-start -->
[Previous: Documentation Home](README.md) | [Documentation Home](README.md) | [Next: Getting Started In 10 Minutes](02-getting-started-in-10-minutes.md)
<!-- docs-nav-end -->

Coredeux is a framework for enterprise Java applications that need a shared,
governed way to handle common work.

It is not just a CRUD library. It is meant to sit around the application so
the recurring parts of enterprise software stay consistent instead of being
scattered across every service.

## The Simple Mental Model

Think of Coredeux like this:

```text
define the capability -> apply framework behavior -> run it through the right backend
```

That is the shape the framework is trying to keep stable.

An application says what it wants to do. Coredeux takes care of the repeatable
parts around that intent.

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

## What The Repository Shows Today

This repository contains the current building blocks for that direction:

- core framework contracts and lifecycle context
- entity definition loading and registry support
- validator, hook, and audit contracts
- JPA, JDBC, MongoDB, Elasticsearch, and Redis data-access implementations
- import and export support
- Spring Boot starter modules for Spring-based hosts
- a native Java demo host for plain Java usage
- a Spring Boot demo host for Spring-based usage

These pieces are useful on their own, but they are also part of a larger story:
Coredeux is shaping the common mechanics so enterprise apps stay predictable as
they grow.

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

## Where To Go Next

If you want the fuller shape of the system, continue with the overview and
then the architecture docs:

- [Overview](overview/README.md)
- [Coredeux Core Reference](modules/coredeux-core/13-reference.md)
- [Lifecycle Model](modules/coredeux-core/02-lifecycle.md)

<!-- docs-nav-start -->
[Previous: Documentation Home](README.md) | [Documentation Home](README.md) | [Next: Getting Started In 10 Minutes](02-getting-started-in-10-minutes.md)
<!-- docs-nav-end -->
