# Coredeux Architecture

<!-- docs-nav-start -->
[Previous: Architecture](README.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Lifecycle Model](lifecycle.md)
<!-- docs-nav-end -->

Coredeux is a modular Java framework that sits between business/application code and the actual persistence implementation.

Its job is to provide one consistent entity lifecycle model across different storage technologies and optional framework capabilities.

## Primary Goal

Coredeux standardizes how an entity is:

- loaded
- validated
- enriched by lifecycle hooks
- audited
- imported
- exported
- delegated to the correct persistence implementation

The framework is intentionally designed so application developers do not need to manually orchestrate these concerns for each persistence technology.

The same architectural direction will extend to agents. Planned MCP capabilities will expose governed framework operations as agent-callable tools, and future agent-aware implementations will carry agent context through the same service, lifecycle, module, and audit boundaries used by human-initiated application flows.

## Core Flow

The current framework flow is:

`CoredeuxService -> CoredeuxStrategy -> Modules -> CoredeuxDataAccessService`

### CoredeuxService

`CoredeuxService` is the public entry point used by business/application code.

It exposes a simplified API and does not require callers to manually provide framework context.

### CoredeuxStrategy

`CoredeuxStrategy` is the framework orchestration layer.

It is responsible for:

- resolving the entity definition
- extracting the configured identifier from the entity
- loading the current persisted state when required
- building request and lifecycle context
- invoking enabled modules
- routing the operation to the correct `CoredeuxDataAccessService`

### Modules

Modules are entity-level framework capabilities that can be enabled or disabled through YAML configuration.

Current built-in module types are:

- `validators`
- `hooks`
- `audit`

Future capabilities can be added using the same extension mechanism.

### CoredeuxDataAccessService

`CoredeuxDataAccessService` is the persistence SPI.

Different implementations can exist for different entity/storage combinations, for example:

- generic JPA
- PostgreSQL-specific JPA with JSONB support
- direct JDBC
- MongoDB
- Elasticsearch
- Redis

The storage adapter is selected from the entity definition. That keeps the
service API stable while allowing different entities to use different backing
stores.

## Lifecycle Model

Coredeux uses a canonical lifecycle vocabulary so all modules and future features work from the same semantics.

Current lifecycle operations are:

- `CREATE`
- `MODIFY`
- `UPSERT`
- `DELETE`
- `FETCH`

These values are framework-level concepts and are propagated through `EntityLifecycleContext`.

### Current semantics

- `save(entity)`
  - before persistence: `CREATE`
  - after persistence: `UPSERT`
- `update(entity)`
  - framework operation: `MODIFY`
- `remove(entity)` / `remove(id, type)`
  - framework operation: `DELETE`
- `load`, `loadAll`, `query`, `refresh`
  - framework operation: `FETCH`

## Context Derivation

Coredeux derives runtime context internally.

The caller does not need to supply an `OperationContext`.

The framework builds:

- `RequestContext`
  derived from the active servlet request when available
- `OperationContext`
  framework-owned runtime context passed to modules
- `EntityLifecycleContext`
  contains operation, identifier, old value, and new value

This is important because it keeps the module contracts consistent and removes orchestration burden from application code.

## Entity Definitions

Entity behavior is driven by YAML definitions.

Each entity definition has a small set of core fields:

- `full-class-name`
- `name`
- `identifier`
- `storage`

Everything else is modeled through the entity's `modules` list.

That structure allows the framework to remain stable while future feature modules add new behavior without changing the core entity contract.

## Current Modules In This Repository

### coredeux-core

Provides:

- core service, strategy, and data-access contracts
- lifecycle context model
- YAML entity definition loading and registry support
- validator, hook, and audit contracts
- built-in module execution infrastructure

### coredeux-core-jpa

Provides:

- generic JPA `CoredeuxDataAccessService`
- PostgreSQL-specific JPA `CoredeuxDataAccessService`
- JSONB-aware native-query handling for PostgreSQL scenarios

### coredeux-core-jdbc

Provides:

- direct SQL/JDBC `CoredeuxDataAccessService`
- table and column mapping from entity metadata
- structured search translated into SQL conditions
- CRUD and query support for relational paths that do not need JPA

### coredeux-core-mongodb

Provides:

- MongoDB-backed `CoredeuxDataAccessService`
- MongoTemplate-based CRUD, query, and structured search support
- mapping from Coredeux entity operations to MongoDB document operations

### coredeux-core-elasticsearch

Provides:

- Elasticsearch-backed `CoredeuxDataAccessService`
- index-based document CRUD
- structured search translated into Elasticsearch query criteria
- paging and query support for catalog/search-oriented entity paths

### coredeux-core-redis

Provides:

- Redis-backed `CoredeuxDataAccessService`
- JSON-backed entity storage
- CRUD, query, and in-memory structured search support
- storage-oriented Redis usage rather than a cache abstraction

### coredeux-import

Provides:

- raw JSON import request and response contracts
- validation-only and execution paths
- create, upsert, modify, delete, and fetch operations
- unique, lookup, and query-based existing-record resolution
- references, row keys, collection modes, macros, and custom value handlers
- pipe-separated text import parsing
- XLS/XLSX import parsing
- alias, metadata, lookup, query, row-key, and multiline value parsing
- a compiler from author-friendly files into `ImportRequest`

### coredeux-export

Provides:

- asynchronous export queueing and worker execution
- structured search and backend query export requests
- text and Excel writers
- value handlers, logs, and storage service extension points

### examples/coredeux-demo

Provides:

- a Spring Boot application that wires the framework modules together
- Docker Compose setup for PostgreSQL, MongoDB, Redis, Elasticsearch, and the
  demo application
- Postman collections for CRUD, import, file import, and export flows
- sample import files for PostgreSQL/JPA, JDBC, MongoDB, Elasticsearch, and
  Redis-backed paths

## Extension Model

The framework is designed so new capabilities can be added in separate modules.

Current extension points include:

- data access services
- backend adapter modules
- module handlers
- validators
- hooks
- audit handlers
- import value handlers
- import parsers
- export value handlers
- export queue, log, storage, and writer services

Planned extension points include MCP capability providers and agent-aware context resolvers so agents can become first-class participants in framework workflows without bypassing validation, lifecycle hooks, persistence routing, or audit behavior.

The intent is that these capabilities use the same lifecycle, definition, and module infrastructure already established in `coredeux-core`.

## Design Direction

Coredeux is not intended to be only a CRUD abstraction.

Its value comes from:

- one lifecycle model across different stores
- per-entity configurable capabilities
- pluggable persistence routing
- framework-owned orchestration instead of repeated application boilerplate

<!-- docs-nav-start -->
[Previous: Architecture](README.md) | [Documentation Home](../README.md) | [Tutorial Order](../SUMMARY.md) | [Next: Lifecycle Model](lifecycle.md)
<!-- docs-nav-end -->
