# Coredeux Core MongoDB Reference

<!-- docs-nav-start -->
[Previous: Coredeux Core Elasticsearch](/16-core-elasticsearch-reference) | [Documentation Home](/) | [Next: Coredeux Core Redis](/18-core-redis-reference)
<!-- docs-nav-end -->

This document is the detailed reference for `coredeux-core-mongodb`.

It explains the MongoDB adapter design, supported operations, search
comparators, and the boundaries between the adapter and the rest of Coredeux.

## Purpose

`coredeux-core-mongodb` is a persistence adapter module for Coredeux.

It depends on `coredeux-core` and supplies a `CoredeuxDataAccessService`
implementation backed by the native MongoDB Java driver.

Current implementation:

- `defaultCoredeuxMongoDataAccessService`

## Why This Module Exists

MongoDB is a good fit when the entity model is document-oriented and you want a
non-relational persistence option that still plugs into the Coredeux SPI.

The adapter uses the native MongoDB Java driver directly rather than Spring
Data repositories so it can stay aligned with Coredeux's dynamic,
entity-definition-driven model.

## Package Structure

Main package:

- `com.coredeux.core.mongodb.service.impl`

Main class:

- [DefaultCoredeuxMongoDataAccessService.java](../../../modules/coredeux-core-mongodb/src/main/java/com/coredeux/core/mongodb/service/impl/DefaultCoredeuxMongoDataAccessService.java)

## Bean Name

The concrete adapter bean name used by the demo and Spring starter is:

- `defaultCoredeuxMongoDataAccessService`

Entity YAML chooses which one to use:

```yaml
storage:
  data-access-service: defaultCoredeuxMongoDataAccessService
```

## MongoDB Adapter Responsibilities

This adapter provides:

- `load`
- `save`
- `update`
- `remove`
- `refresh`
- `loadAll` using native MongoDB filters
- `query` using a JSON template with `{{param}}` placeholders
- identifier conversion from String into the entity identifier type
- pagination and result counting
- framework exception wrapping

## Transaction Model

The implementation is native-driver based. In Spring Boot apps, the starter
wires the bean into the application context, but the data-access code itself is
not based on Spring Data MongoDB.

## Method Behavior

### `load(String id, Class<T> type)`

Behavior:

- validate id and type
- resolve the entity identifier type from the identifier field or `id` field
- convert the incoming String id into that type
- query the collection by identifier through the native driver

### `save(T entity)`

Behavior:

- validate non-null entity
- convert the entity to a BSON document and insert or replace it through the
  native driver
- read the identifier back from the saved entity
- return the identifier as a string when available

### `update(T entity)`

Behavior:

- validate non-null entity
- require an identifier before updating
- replace the document through the native driver

### `remove(T entity)`

Behavior:

- validate non-null entity
- require an identifier before removal
- delete the document through the native driver

### `refresh(T entity)`

Behavior:

- validate non-null entity
- require an identifier
- reload the entity from MongoDB
- copy the refreshed state back into the provided instance when a record exists

### `loadAll(...)`

Behavior:

- validate type
- build a Mongo filter from `SearchParams`
- count total documents in the collection
- count filtered matches
- apply paging
- return `SearchResult<T>`

### `query(...)`

Behavior:

- validate query and type
- replace `{{param}}` placeholders with JSON literals
- execute the query with MongoDB JSON syntax through `Document.parse(...)`
- count matching documents
- apply paging
- return `SearchResult<T>`

## Search Comparator Support

The MongoDB adapter supports these comparators in `loadAll(...)`:

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

Comparator support is adapter-specific. MongoDB advertises its own supported
set through `supportedComparators(Class<?> type)`, so the selected
data-access-service bean is the source of truth for what search operators the
framework should accept.

## Query Template Syntax

`query(...)` accepts JSON-style MongoDB query documents with template
placeholders.

Example:

```java
dataAccessService.query(
    "{\"name\": {{name}}, \"age\": {\"$gte\": {{minAge}}}}",
    Map.of("name", "Alpha", "minAge", 18),
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
- if no dedicated field exists, it falls back to a field named `id`
- incoming String ids are converted to common identifier types such as
  `String`, `ObjectId`, `UUID`, numeric wrappers, `BigInteger`, and
  `BigDecimal`

## Choosing The Right Adapter

Choose the data access service in the entity YAML definition.

MongoDB entity:

```yaml
storage:
  data-access-service: defaultCoredeuxMongoDataAccessService
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
mvn -pl modules/coredeux-core-mongodb -am test
```

## Who Should Depend On This Module

Depend on `coredeux-core-mongodb` if your application or module:

- uses MongoDB as the persistence layer
- wants a ready `CoredeuxDataAccessService` implementation
- needs storage-specific search comparators exposed through
  `supportedComparators(Class<?> type)`

<!-- docs-nav-start -->
[Previous: Coredeux Core Elasticsearch](/16-core-elasticsearch-reference) | [Documentation Home](/) | [Next: Coredeux Core Redis](/18-core-redis-reference)
<!-- docs-nav-end -->
