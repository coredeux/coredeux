# Coredeux Core Elasticsearch Reference

<!-- docs-nav-start -->
[Previous: Coredeux Core JDBC](../core-jdbc/reference.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Coredeux Core MongoDB](../core-mongodb/reference.md)
<!-- docs-nav-end -->

This document is the detailed reference for `coredeux-core-elasticsearch`.

It explains the Elasticsearch adapter design, supported operations, search
comparators, and the boundaries between the adapter and the rest of Coredeux.

## Purpose

`coredeux-core-elasticsearch` is a persistence adapter module for Coredeux.

It depends on `coredeux-core` and supplies a `CoredeuxDataAccessService`
implementation backed by `ElasticsearchOperations`.

Current implementation:

- `defaultCoredeuxElasticsearchDataAccessService`

## Why This Module Exists

Elasticsearch is a good fit when the entity model needs search-oriented
storage and text-friendly querying while still plugging into the Coredeux SPI.

The adapter uses `ElasticsearchOperations` directly so it can stay aligned with
Coredeux's dynamic, entity-definition-driven model.

## Package Structure

Main package:

- `com.coredeux.core.elasticsearch.service.impl`

Main class:

- [DefaultCoredeuxElasticsearchDataAccessService.java](../../../modules/coredeux-core-elasticsearch/src/main/java/com/coredeux/core/elasticsearch/service/impl/DefaultCoredeuxElasticsearchDataAccessService.java)

## Spring Bean

This module exposes one `CoredeuxDataAccessService` bean:

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

The implementation uses Spring transactions:

- `@Transactional(readOnly = true)` for `load`, `loadAll`, `query`, and `refresh`
- `@Transactional` for `save`, `update`, and `remove`

## Method Behavior

### `load(String id, Class<T> type)`

Behavior:

- validate id and type
- resolve the entity identifier type from the `@Id` field or `id` field
- convert the incoming String id into that type
- call `elasticsearchOperations.get(...)`

### `save(T entity)`

Behavior:

- validate non-null entity
- call `elasticsearchOperations.save(...)`
- read the identifier back from the saved entity
- return the identifier as a string when available

### `update(T entity)`

Behavior:

- validate non-null entity
- call `elasticsearchOperations.save(...)`

### `remove(T entity)`

Behavior:

- validate non-null entity
- call `elasticsearchOperations.delete(...)`

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

- the adapter looks for a field annotated with Spring Data Elasticsearch `@Id`
- if no annotated field exists, it falls back to a field named `id`
- incoming String ids are converted to common identifier types such as
  `String`, `UUID`, numeric wrappers, `BigInteger`, and `BigDecimal`

## Index Handling

The adapter resolves index coordinates from Spring Data Elasticsearch when
available and otherwise falls back to the entity simple name.

You can also set a prefix with:

```yaml
coredeux:
  elasticsearch:
    default-index-prefix: coredeux
```

That lets the module target a consistent index naming scheme without changing
the entity type.

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
[Previous: Coredeux Core JDBC](../core-jdbc/reference.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Coredeux Core MongoDB](../core-mongodb/reference.md)
<!-- docs-nav-end -->
