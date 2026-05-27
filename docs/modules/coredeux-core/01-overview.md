# Overview

<!-- docs-nav-start -->
[Previous: Coredeux Core](/coredeux-core) | [Documentation Home](/) | [Next: Lifecycle Model](/02-lifecycle)
<!-- docs-nav-end -->

Coredeux Core is the contract layer between application intent and backend
execution.

That is the simplest way to think about it.

Application code says what it wants to do. Coredeux coordinates how that work
should flow through validation, hooks, audit, module handlers, and the chosen
storage adapter.

## A Simple Mental Model

- `CoredeuxService` is the public entry point.
- `CoredeuxStrategy` coordinates the framework work.
- `CoredeuxDataAccessService` is the storage adapter boundary.
- modules add behavior around the lifecycle.

That model matters because Coredeux is not just a helper class and not just a
persistence wrapper. It is the framework layer that keeps common enterprise
behavior consistent.

## Why Coredeux Is Relevant Now

Spring Boot is still the default starting point for many Java enterprise
applications. That will not disappear overnight.

What changes over time is that those applications will increasingly need to
work alongside AI tools and agent-driven workflows. In practice, that means
future applications need a stable framework surface that agents and developers
can both understand.

Coredeux is relevant because it gives that surface a shape:

- a consistent lifecycle model
- standard CRUD and search behavior
- repeatable import and export flows
- configurable validation, hooks, and audit
- pluggable backend routing
- a way to embed into existing Java or Spring applications without rewriting
  the whole system

AI tools can generate code, but real enterprise applications still need a
predictable runtime contract. Coredeux exists to make that contract less ad hoc
and more reusable.

## What Coredeux Is For

Coredeux standardizes enterprise building blocks that tend to get repeated in
every serious application:

- CRUD
- validation
- hooks
- audit
- import
- export
- backend routing
- module-driven extensions

The goal is not to replace application logic. The goal is to make the common
framework behavior predictable so application code can stay focused on the
business rules that are unique to the project.

## Where The Framework Lives Today

The current repository already contains working pieces of the broader Coredeux
vision:

- `coredeux-core`
- `coredeux-core-jpa`
- `coredeux-core-jdbc`
- `coredeux-core-elasticsearch`
- `coredeux-core-mongodb`
- `coredeux-core-redis`
- `coredeux-import`
- `coredeux-export`
- the Spring Boot demo
- the native demo

The code base is still evolving, so this document should be read as the
framework story and direction, not as a claim that every future module already
exists.

## The Reading Path

If you are new to the core framework, read the pages in this order:

1. this overview
2. lifecycle
3. entity definitions
4. module system
5. the reference page when you need deeper detail

<!-- docs-nav-start -->
[Previous: Coredeux Core](/coredeux-core) | [Documentation Home](/) | [Next: Lifecycle Model](/02-lifecycle)
<!-- docs-nav-end -->
