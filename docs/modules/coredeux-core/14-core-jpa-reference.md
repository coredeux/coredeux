# coredeux-core-jpa Reference

<!-- docs-nav-start -->
[Previous: coredeux-core Reference](/modules/coredeux-core/13-reference) | [Documentation Home](/) | [Next: Coredeux Core JDBC](/modules/coredeux-core/15-core-jdbc-reference)
<!-- docs-nav-end -->

This document is the detailed reference for `coredeux-core-jpa`.

It explains the current JPA adapter design, supported operations, search
comparators, PostgreSQL JSONB support, and the boundaries between the generic
and PostgreSQL-specific implementations.

## Purpose

`coredeux-core-jpa` is a persistence adapter module for Coredeux.

It depends on `coredeux-core` and supplies `CoredeuxDataAccessService`
implementations backed by JPA/Hibernate.

Current implementations:

- `defaultCoredeuxJpaDataAccessService`
- `postgresCoredeuxJpaDataAccessService`

## Why There Are Two Implementations

The module intentionally separates:

- generic JPA behavior
- PostgreSQL-specific JSONB/native-query behavior

That split keeps the default JPA adapter storage-agnostic and prevents JSONB
support from leaking PostgreSQL concerns into the generic implementation.

## Package Structure

Main packages:

- `com.coredeux.core.jpa.service.impl`
- `com.coredeux.core.jpa.support`

Main classes:

- [DefaultCoredeuxJpaDataAccessService.java](../../../modules/coredeux-core-jpa/src/main/java/com/coredeux/core/jpa/service/impl/DefaultCoredeuxJpaDataAccessService.java)
- [PostgresCoredeuxJpaDataAccessService.java](../../../modules/coredeux-core-jpa/src/main/java/com/coredeux/core/jpa/service/impl/PostgresCoredeuxJpaDataAccessService.java)
- [JpaIdentifierConverter.java](../../../modules/coredeux-core-jpa/src/main/java/com/coredeux/core/jpa/support/JpaIdentifierConverter.java)

## Spring Beans

This module exposes two `CoredeuxDataAccessService` beans:

- `defaultCoredeuxJpaDataAccessService`
- `postgresCoredeuxJpaDataAccessService`

Entity YAML chooses which one to use:

```yaml
storage:
  data-access-service: defaultCoredeuxJpaDataAccessService
```

or:

```yaml
storage:
  data-access-service: postgresCoredeuxJpaDataAccessService
```

## Spring Boot Opt-In

Adding the JPA Spring Boot starter dependency is not enough to create these
beans. Enable the starter explicitly:

```yaml
coredeux:
  jpa:
    enabled: true
```

When `coredeux.jpa.enabled=true`, the starter creates the JPA data-access
beans only if an `EntityManagerFactory` is already available. If an entity or
the global `coredeux.data-access-service` fallback uses
`defaultCoredeuxJpaDataAccessService` or `postgresCoredeuxJpaDataAccessService`,
the JPA datasource must be configured and reachable.

## Generic JPA Adapter

Class:
- [DefaultCoredeuxJpaDataAccessService.java](../../../modules/coredeux-core-jpa/src/main/java/com/coredeux/core/jpa/service/impl/DefaultCoredeuxJpaDataAccessService.java)

Bean name:
- `defaultCoredeuxJpaDataAccessService`

### Responsibilities

This adapter provides:

- `load`
- `save`
- `update`
- `remove`
- `refresh`
- `loadAll` via Criteria API
- `query` via JPQL
- identifier conversion from String into entity id type
- pagination and result counting
- framework exception wrapping

### Transaction model

The implementation uses Spring transactions:

- `@Transactional(readOnly = true)` for `load`, `loadAll`, `query`
- `@Transactional` for `save`, `update`, `remove`, `refresh`

### EntityManager usage

It uses:

- `@PersistenceContext EntityManager`

The adapter is based directly on `EntityManager`, not Spring Data repositories.

### Method behavior

#### `load(String id, Class<T> type)`

Behavior:

- validate id and type
- resolve JPA identifier type
- convert incoming string id to that type
- call `entityManager.find(...)`
- detach the entity if found
- return detached entity or `null`

Important implication:

- returned entities are detached
- this avoids accidental dirty-state persistence
- lazy JPA relationships remain a transport-layer concern

#### `save(T entity)`

Behavior:

- validate non-null entity
- `persist`
- `flush`
- resolve identifier from JPA persistence unit utilities
- return identifier as string when available

#### `update(T entity)`

Behavior:

- validate non-null entity
- `merge`
- `flush`

#### `remove(T entity)`

Behavior:

- validate non-null entity
- if entity is not managed, `merge` first
- `remove`
- `flush`

#### `refresh(T entity)`

Behavior:

- validate non-null entity
- if not managed, `merge`
- `refresh`

#### `query(...)`

Behavior:

- validate query and type
- run the JPQL query once to determine result size
- run it again with pagination applied
- bind named parameters from the provided map
- detach results
- build `SearchResult<T>`

Important note:

- total count is currently calculated by executing the full query and taking the
  result size, not by generating a separate count query

#### `loadAll(...)`

Behavior:

- validate type
- build Criteria predicates from `SearchParams`
- execute a paged criteria query
- detach results
- count total entity rows
- count filtered rows using a separate criteria count query
- return `SearchResult<T>`

### Search comparator support

The generic JPA adapter supports these comparators in `loadAll(...)`:

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

Comparator support is adapter-specific. Other persistence modules advertise
their own supported set through `supportedComparators(Class<?> type)`, so the
selected data-access service is the source of truth for what search operators
the framework should accept.

Comparator semantics:

- `ANYWHERECS`
  contains search, case-sensitive
- `ANYWHERE`
  contains search, case-insensitive
- `STARTSWITH`
  prefix search
- `CONTAINS`
  membership test on collection-valued fields
- `NOTCONTAINS`
  inverse membership test on collection-valued fields
- `ISEMPTY`
  collection is empty
- `ISNOTEMPTY`
  collection is not empty

### Nested fields

`resolvePath(...)` supports dot-separated field traversal such as:

```text
customer.status
profile.locale
```

This works by repeatedly calling `Path.get(...)`.

### Validation rules in the generic adapter

Current important validation:

- load id must not be blank
- query string must not be blank
- entity type must not be null
- entity must not be null for save/update/remove/refresh
- composite identifiers are not supported
- unsupported comparators cause `CoredeuxValidationException`
- collection comparators require collection-valued fields
- comparison operators require `Comparable` values

### Detach behavior

The adapter detaches:

- entities returned by `load`
- each result entity returned by `query`
- each result entity returned by `loadAll`

This is intentional and keeps Coredeux from leaking JPA managed-state behavior
through the public framework service.

Tradeoff:

- you avoid accidental automatic persistence of object mutations
- you must handle lazy JPA serialization concerns at the transport layer

## PostgreSQL JPA Adapter

Class:
- [PostgresCoredeuxJpaDataAccessService.java](../../../modules/coredeux-core-jpa/src/main/java/com/coredeux/core/jpa/service/impl/PostgresCoredeuxJpaDataAccessService.java)

Bean name:
- `postgresCoredeuxJpaDataAccessService`

This adapter extends `DefaultCoredeuxJpaDataAccessService`.

### Responsibilities

It adds:

- PostgreSQL JSONB-aware search in `loadAll(...)`
- native SQL fallback when JSONB comparators are present

If no JSONB comparators are present, it delegates directly to the generic
adapter behavior.

### JSONB comparators

Currently supported:

- `JSONB(TEXT)`
- `JSONB(NUMERIC)`

### How `loadAll(...)` behaves in the PostgreSQL adapter

1. inspect search params
2. if no JSONB comparators exist:
   use generic Criteria-based implementation
3. if JSONB comparators exist:
   build native SQL
4. execute paged native query
5. count filtered rows using native SQL
6. detach returned entities

### Standard comparator support in native JSONB mode

When native mode is active, non-JSON comparators in the same request are still
supported, but only through the native-condition builder.

Current native standard comparator support:

- `EQUALS`
- `NOTEQUALS`
- `STARTSWITH`
- `ANYWHERECS`
- `ANYWHERE`
- `LESSTHAN`
- `LESSTHANOREQUAL`
- `GREATERTHAN`
- `GREATERTHANOREQUAL`
- `ISNULL`
- `ISNOTNULL`

Collection comparators are not handled by the native PostgreSQL JSONB search
path.

### Table and column resolution

Table name resolution:

- uses `@Table(name = "...")` when present
- otherwise falls back to `type.getSimpleName()`

Column name resolution:

- uses reflection to find the field in the class hierarchy
- uses `@Column(name = "...")` when present
- otherwise relies on the effective identifier resolved by Coredeux from the
  entity definition or its storage fallback

### JSONB text expression styles

There are two supported styles.

#### 1. Direct SQL expression style

Use this when you want explicit control over the JSON expression.

Example:

```java
SearchParams.builder()
    .field("payload->>'name'")
    .comparator("JSONB(TEXT)")
    .value("= 'alpha'")
    .build();
```

The adapter detects JSON operators already present in the field expression and
prefixes the base column with the table alias.

#### 2. Dot-notation style

Use this when you want the adapter to build the PostgreSQL path expression.

Example:

```java
SearchParams.builder()
    .field("payload.customer.name")
    .comparator("JSONB(TEXT)")
    .value(Map.of(
        "operator", "ANYWHERE",
        "value", "john"))
    .build();
```

The adapter translates dot notation into PostgreSQL `#>>` path extraction.

### JSONB text comparator input

`JSONB(TEXT)` accepts two value styles.

#### Raw string condition

Example:

```java
SearchParams.builder()
    .field("payload->>'name'")
    .comparator("JSONB(TEXT)")
    .value("= 'alpha'")
    .build();
```

This is appended directly after the generated JSON expression.

#### Map-based operator/value form

Example:

```java
Map.of(
    "operator", "ANYWHERE",
    "value", "john")
```

Supported map operators for `JSONB(TEXT)`:

- `EQUALS`
- `NOTEQUALS`
- `STARTSWITH`
- `ANYWHERECS`
- `ANYWHERE`

### JSONB numeric comparator input

`JSONB(NUMERIC)` accepts two value styles.

#### Raw SQL template string

Example:

```java
SearchParams.builder()
    .field("payload.metrics.age")
    .comparator("JSONB(NUMERIC)")
    .value("cast(%s as numeric) > 18")
    .build();
```

The `%s` placeholder is replaced with the generated JSON text expression.

#### Map-based operator/value form

Example:

```java
Map.of(
    "operator", "GREATERTHAN",
    "value", 18)
```

Supported map operators for `JSONB(NUMERIC)`:

- `EQUALS`
- `NOTEQUALS`
- `LESSTHAN`
- `LESSTHANOREQUAL`
- `GREATERTHAN`
- `GREATERTHANOREQUAL`

### Native SQL parameterization

Map-based JSONB conditions and standard native conditions use positional query
parameters.

That is safer than concatenating values into SQL strings.

Raw string/template styles are intentionally more flexible, but they also shift
more responsibility to the caller.

Recommended guidance:

- prefer the map-based style for new code
- use direct SQL expression style only when you need exact control

## `JpaIdentifierConverter`

Class:
- [JpaIdentifierConverter.java](../../../modules/coredeux-core-jpa/src/main/java/com/coredeux/core/jpa/support/JpaIdentifierConverter.java)

Purpose:

- convert incoming `String` identifiers from `CoredeuxService.load(...)` into
  the actual JPA identifier type

Built-in support includes:

- `String`
- `Long`, `long`
- `Integer`, `int`
- `Short`, `short`
- `Byte`, `byte`
- `Double`, `double`
- `Float`, `float`
- `BigInteger`
- `BigDecimal`
- `UUID`
- `Enum`

Fallback behavior:

1. try static `valueOf(String)`
2. try constructor `(String)`
3. otherwise fail with `CoredeuxValidationException`

## Typical Configuration

### Generic JPA entity

```yaml
coredeux:
  entities:
    - full-class-name: com.example.customer.Customer
      identifier: pk
      storage:
        data-access-service: defaultCoredeuxJpaDataAccessService
```

### PostgreSQL JSONB-aware entity

```yaml
coredeux:
  entities:
    - full-class-name: com.example.catalog.Product
      identifier: pk
      storage:
        data-access-service: postgresCoredeuxJpaDataAccessService
```

## Common Search Examples

### Generic string match

```java
List<SearchParams> params = List.of(
    SearchParams.builder()
        .field("name")
        .comparator("ANYWHERE")
        .value("charlie")
        .build()
);
```

### Generic collection membership

```java
List<SearchParams> params = List.of(
    SearchParams.builder()
        .field("tags")
        .comparator("CONTAINS")
        .value("premium")
        .build()
);
```

### PostgreSQL JSONB text search

```java
List<SearchParams> params = List.of(
    SearchParams.builder()
        .field("payload.customer.name")
        .comparator("JSONB(TEXT)")
        .value(Map.of(
            "operator", "ANYWHERE",
            "value", "john"))
        .build()
);
```

### PostgreSQL JSONB numeric search

```java
List<SearchParams> params = List.of(
    SearchParams.builder()
        .field("payload.metrics.age")
        .comparator("JSONB(NUMERIC)")
        .value(Map.of(
            "operator", "GREATERTHAN",
            "value", 18))
        .build()
);
```

## Testing Strategy In This Module

Current tests cover:

- H2-backed integration tests for the generic adapter
- PostgreSQL JSONB query-building behavior
- identifier conversion

Run with:

```powershell
mvn -pl modules/coredeux-core-jpa -am test
```

## Current Boundaries and Limitations

Important current limits:

- no composite identifier support in the generic adapter
- generic `query(...)` uses JPQL and does not auto-generate count queries
- PostgreSQL JSONB support is focused on `loadAll(...)`
- JSONB search support is PostgreSQL-specific, not portable JPA behavior
- native JSONB mode does not try to support every generic comparator

Also note:

- detached entities returned by the adapter may still contain lazy JPA
  relationships depending on the entity mapping
- that is a transport/representation concern, not a `coredeux-core-jpa`
  lifecycle concern

## Recommended Reading Order

If you are adopting Coredeux with JPA:

1. read the `coredeux-core` reference first
2. read this document
3. configure entity YAML with the correct JPA bean
4. add validators/hooks/audit only after the data-access path is working

<!-- docs-nav-start -->
[Previous: coredeux-core Reference](/modules/coredeux-core/13-reference) | [Documentation Home](/) | [Next: Coredeux Core JDBC](/modules/coredeux-core/15-core-jdbc-reference)
<!-- docs-nav-end -->
