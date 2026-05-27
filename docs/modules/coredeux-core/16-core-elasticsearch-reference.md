# Coredeux Core Elasticsearch Reference

<!-- docs-nav-start -->
[Previous: Coredeux Core JDBC](/15-core-jdbc-reference) | [Documentation Home](/) | [Next: Coredeux Core MongoDB](/17-core-mongodb-reference)
<!-- docs-nav-end -->

This document is the detailed reference for `coredeux-core-elasticsearch`.

It explains the Elasticsearch adapter design, supported operations, search
comparators, and the boundaries between the adapter and the rest of Coredeux.

This page also carries the quick module-level overview that used to live in the
older README, so the search-first positioning and the adapter details stay
together.

## Purpose

`coredeux-core-elasticsearch` is a persistence adapter module for Coredeux.

It depends on `coredeux-core` and supplies a `CoredeuxDataAccessService`
implementation backed by the native Elasticsearch Java API client.

Current implementation:

- `defaultCoredeuxElasticsearchDataAccessService`

## Why This Module Exists

Elasticsearch is a good fit when the entity model needs search-oriented
storage and text-friendly querying while still plugging into the Coredeux SPI.

The adapter uses the native Elasticsearch Java API client directly so it can
stay aligned with Coredeux's dynamic, entity-definition-driven model without
depending on Spring Data Elasticsearch.

## Package Structure

Main package:

- `com.coredeux.core.elasticsearch.service.impl`

Main class:

- [DefaultCoredeuxElasticsearchDataAccessService.java](../../../modules/coredeux-core-elasticsearch/src/main/java/com/coredeux/core/elasticsearch/service/impl/DefaultCoredeuxElasticsearchDataAccessService.java)

## Bean Name

The concrete adapter bean name used by the demo and Spring starter is:

- `defaultCoredeuxElasticsearchDataAccessService`

Entity YAML chooses which one to use:

```yaml
storage:
  data-access-service: defaultCoredeuxElasticsearchDataAccessService
```

## Elasticsearch Adapter Responsibilities

This adapter provides:

- `load`
- `save`
- `update`
- `remove`
- `refresh`
- `loadAll` using Elasticsearch criteria
- `query` using a JSON template with `{{param}}` placeholders
- identifier conversion from String into the entity identifier type
- pagination and result counting
- framework exception wrapping

## Transaction Model

The implementation is the native client-backed adapter. In the Spring Boot
starter it is wrapped in the normal Spring bean lifecycle, but the adapter
itself does not depend on Spring Data Elasticsearch.

## Method Behavior

### `load(String id, Class<T> type)`

Behavior:

- validate id and type
- resolve the entity identifier type from the identifier field or `id` field
- convert the incoming String id into that type
- call the native gateway to load the document

### `save(T entity)`

Behavior:

- validate non-null entity
- index the entity through the native gateway
- read the identifier back from the saved entity
- return the identifier as a string when available

### `update(T entity)`

Behavior:

- validate non-null entity
- re-index the entity through the native gateway

### `remove(T entity)`

Behavior:

- validate non-null entity
- delete the document through the native gateway

### `refresh(T entity)`

Behavior:

- validate non-null entity
- require an identifier
- reload the entity from Elasticsearch
- copy the refreshed state back into the provided instance when a record exists

### `loadAll(...)`

Behavior:

- validate type
- build an Elasticsearch `Query` from `SearchParams`
- count total documents in the index
- count filtered matches
- apply paging
- return `SearchResult<T>`

### `query(...)`

Behavior:

- validate query and type
- replace `{{param}}` placeholders with JSON literals
- execute the query as Elasticsearch JSON
- count matching documents
- apply paging
- return `SearchResult<T>`

## Search Comparator Support

The Elasticsearch adapter supports these comparators in `loadAll(...)`:

- `EQUALS`
- `NOTEQUALS`
- `STARTSWITH`
- `ANYWHERECS`
- `ANYWHERE`
- `LESSTHANOREQUAL`
- `LESSTHAN`
- `GREATERTHANOREQUAL`
- `GREATERTHAN`
- `ISNULL`
- `ISNOTNULL`
- `ISEMPTY`
- `ISNOTEMPTY`
- `CONTAINS`
- `NOTCONTAINS`

Comparator support is adapter-specific. Elasticsearch advertises its own
supported set through `supportedComparators(Class<?> type)`, so the selected
data-access-service bean is the source of truth for what search operators the
framework should accept.

## Query Template Syntax

`query(...)` accepts Elasticsearch JSON documents with template placeholders.

Example:

```java
dataAccessService.query(
    "{\"query\":{\"term\":{\"name\":{{name}}}}}",
    Map.of("name", "Alpha"),
    CustomerDocument.class,
    20,
    1);
```

Rules:

- placeholders use `{{paramName}}`
- string values are JSON-quoted automatically
- numbers, booleans, maps, lists, and null are serialized as JSON literals
- missing placeholders raise a validation error

## Identifier Handling

Identifier resolution is reflection-based:

- the adapter looks for an identifier field
- if no dedicated field is found, it falls back to a field named `id`
- incoming String ids are converted to common identifier types such as
  `String`, `UUID`, numeric wrappers, `BigInteger`, and `BigDecimal`

## Index Handling

The adapter resolves index names from the entity type and optional
`default-index-prefix`.

You can also set a prefix with:

```yaml
coredeux:
  elasticsearch:
    default-index-prefix: coredeux
```

That lets the module target a consistent index naming scheme without changing
the entity type.

The native gateway uses the Elasticsearch client directly, so the application
only needs to provide the client and the bean name. Spring Boot starters wire
that automatically when you use the starter modules.

## Choosing The Right Adapter

Choose the data access service in the entity YAML definition.

Elasticsearch entity:

```yaml
storage:
  data-access-service: defaultCoredeuxElasticsearchDataAccessService
```

That allows different entities to use different persistence implementations
inside the same application.

## Testing

This module includes unit tests for:

- CRUD method wiring
- comparator support
- query template substitution
- identifier conversion
- pagination behavior

Run the module tests with dependencies:

```powershell
mvn -pl modules/coredeux-core-elasticsearch -am test
```

## Who Should Depend On This Module

Depend on `coredeux-core-elasticsearch` if your application or module:

- uses Elasticsearch as the persistence or search layer
- wants a ready `CoredeuxDataAccessService` implementation
- needs storage-specific search comparators exposed through
  `supportedComparators(Class<?> type)`

<!-- docs-nav-start -->
[Previous: Coredeux Core JDBC](/15-core-jdbc-reference) | [Documentation Home](/) | [Next: Coredeux Core MongoDB](/17-core-mongodb-reference)
<!-- docs-nav-end -->
