# coredeux-core-jpa

`coredeux-core-jpa` provides JPA-based implementations of `CoredeuxDataAccessService`.

This module depends on `coredeux-core` and is intended for applications that want to use JPA/Hibernate as the persistence adapter behind Coredeux.

Detailed reference:

- [docs/modules/core-jpa/reference.md](/C:/Data/Development/Coredeux/coredeux-oss/coredeux/docs/modules/core-jpa/reference.md)

## What This Module Provides

This module currently provides two Spring beans implementing `CoredeuxDataAccessService`:

- `defaultCoredeuxJpaDataAccessService`
  generic JPA/Criteria-based implementation
- `postgresCoredeuxJpaDataAccessService`
  PostgreSQL-specific implementation with JSONB/native-query support

## Why There Are Two Implementations

The generic JPA adapter is intentionally database-agnostic.

It supports standard CRUD, JPQL queries, Criteria-based filtering, and pagination without introducing PostgreSQL-specific query behavior into the default implementation.

The PostgreSQL adapter exists because JSONB support is not a portable JPA feature and, in practice, requires native SQL for expressive filtering.

This split keeps the generic adapter clean while still allowing PostgreSQL-specific capabilities where needed.

## Generic JPA Adapter

Class:

- `DefaultCoredeuxJpaDataAccessService`

Features:

- `load`
- `save`
- `update`
- `remove`
- `refresh`
- `loadAll` using Criteria API
- `query` using JPQL with named parameters
- identifier conversion from string to the actual entity identifier type
- framework exception wrapping

Supported comparators in `loadAll`:

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

Comparator support is adapter-specific. Other persistence modules, including
MongoDB, advertise their own comparator set through
`supportedComparators(Class<?> type)`.

## PostgreSQL JPA Adapter

Class:

- `PostgresCoredeuxJpaDataAccessService`

The PostgreSQL adapter extends the generic implementation and only switches behavior when JSONB comparators are present.

If no JSONB comparators are provided, it delegates to the generic JPA implementation.

### JSONB support

Supported JSONB comparators:

- `JSONB(TEXT)`
- `JSONB(NUMERIC)`

### Supported input styles

The PostgreSQL adapter supports two styles for JSONB conditions.

#### 1. Direct SQL expression style

This style gives the caller explicit control over the JSONB expression used in the native query.

It is useful when you already know the exact PostgreSQL JSONB path or condition shape you want to apply.

Examples:

```java
SearchParams.builder()
    .field("payload->>'name'")
    .comparator("JSONB(TEXT)")
    .value("= 'alpha'")
    .build();
```

```java
SearchParams.builder()
    .field("payload.metrics.age")
    .comparator("JSONB(NUMERIC)")
    .value("cast(%s as numeric) > 18")
    .build();
```

#### 2. Map-based parameterized style

This style is more structured and generally safer for new code.

It is a better fit when you want the framework to build the comparison from an operator and a value instead of writing the SQL condition manually.

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

Example numeric:

```java
SearchParams.builder()
    .field("payload.customer.age")
    .comparator("JSONB(NUMERIC)")
    .value(Map.of(
        "operator", "GREATERTHAN",
        "value", 18))
    .build();
```

## How To Select The Right Adapter Per Entity

Choose the data access service in the entity YAML definition.

Generic JPA entity:

```yaml
storage:
  data-access-service: defaultCoredeuxJpaDataAccessService
```

PostgreSQL JSONB-aware entity:

```yaml
storage:
  data-access-service: postgresCoredeuxJpaDataAccessService
```

That allows different entities to use different persistence implementations even inside the same application.

## Testing

This module includes:

- H2-backed integration tests for the generic JPA adapter
- unit/integration-style tests for PostgreSQL JSONB query construction and fallback behavior
- identifier conversion tests

Run the module tests with dependencies:

```powershell
mvn -pl modules/coredeux-core-jpa -am test
```

## Current Boundaries

The generic adapter is production-usable for normal JPA scenarios.

The PostgreSQL adapter currently covers JSONB native-query generation and the framework-facing behavior around it. If you want database-level confidence for real PostgreSQL JSONB execution semantics, the next step should be a dedicated PostgreSQL integration test profile.

## Who Should Depend On This Module

Depend on `coredeux-core-jpa` if your application or module:

- uses JPA/Hibernate as the persistence layer
- wants a ready `CoredeuxDataAccessService` implementation
- needs PostgreSQL JSONB support in Coredeux search/filter scenarios
