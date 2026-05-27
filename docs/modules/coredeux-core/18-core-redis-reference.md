# Coredeux Core Redis Reference

<!-- docs-nav-start -->
[Previous: Coredeux Core MongoDB](/17-core-mongodb-reference) | [Documentation Home](/) | [Next: Property Resolution Order](/19-property-resolution-order)
<!-- docs-nav-end -->

`coredeux-core-redis` is the Redis adapter for Coredeux.

It supplies a `CoredeuxDataAccessService` implementation backed by Lettuce and
stores entity payloads as JSON strings under predictable Redis keys.

This module currently behaves as Redis-backed storage, not as a cache layer.
It persists JSON payloads, reads them back by key, and performs scan-based
search over the stored namespace.

This page also carries the module-level Redis summary that used to live in the
older README, so the storage-first positioning and the detailed adapter notes
now live together.

## Purpose

This module exists for cases where you want Coredeux to work with Redis as a
simple operational store rather than as a full secondary query engine.

Current implementation:

- `defaultCoredeuxRedisDataAccessService`

## How It Stores Data

The adapter stores each entity as a JSON string.

Key layout:

- namespace: `<prefix>:<EntitySimpleName>` when a prefix is configured
- entity key: `<namespace>:<id>`
- sequence key: `<namespace>:seq`

The prefix is controlled by:

```yaml
coredeux:
  redis:
    default-key-prefix: demo
```

If the prefix is blank, the namespace falls back to the entity simple name.

## Spring Bean

This module exposes one `CoredeuxDataAccessService` bean:

- `defaultCoredeuxRedisDataAccessService`

Choose it from the entity storage configuration:

```yaml
storage:
  data-access-service: defaultCoredeuxRedisDataAccessService
```

## What It Supports

The adapter provides:

- `load`
- `save`
- `update`
- `remove`
- `refresh`
- `loadAll`
- `query`
- `supportedComparators`

## Comparator Support

Redis advertises the comparators that its adapter can evaluate in-memory:

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

Comparator support is adapter-specific. Coredeux should validate search requests
against the selected data-access service, not against a generic framework-wide
list.

## Query Style

`query(...)` accepts JSON input with filter definitions.

Supported forms:

- an array of filters
- an object with a `filters` array
- a single filter object with `field`, `comparator`, and `value`

Example:

```java
dataAccessService.query(
    """
    {"filters":[{"field":"name","comparator":"ANYWHERE","value":"ha"}]}
    """,
    Map.of(),
    CustomerDocument.class,
    20,
    1);
```

Values are resolved through `{{param}}` placeholders before the JSON is parsed.

## Identifier Handling

The adapter resolves an identifier field using either:

- Spring Data `@Id`
- a field named `id`

When an entity is saved without an identifier, Redis generates one:

- `String` and `UUID` identifiers use UUID values
- numeric identifiers use a Redis sequence key
- unsupported identifier types fall back to a generated string value

## Behavior Notes

This adapter is intentionally simple:

- searches are performed by scanning the Redis namespace and filtering in
  memory
- JSON is used for persistence and query payloads
- it is best suited to bounded datasets or demo-style use cases

Because of that, the current module should be understood as storage first. It
is not a drop-in cache implementation.

If you need Redis-native search indexing or a larger-scale query model, this
adapter is not trying to replace a dedicated Redis search solution.

## Testing

The module includes unit tests for:

- CRUD behavior
- comparator support
- JSON query parsing
- identifier generation
- paging
- validation and error wrapping

Run the module tests with dependencies:

```powershell
mvn -pl modules/coredeux-core-redis -am test
```

## When To Use This Module

Use `coredeux-core-redis` when you want:

- a Redis-backed `CoredeuxDataAccessService`
- simple JSON persistence
- predictable key layout
- module-level wiring that still fits the Coredeux SPI

<!-- docs-nav-start -->
[Previous: Coredeux Core MongoDB](/17-core-mongodb-reference) | [Documentation Home](/) | [Next: Property Resolution Order](/19-property-resolution-order)
<!-- docs-nav-end -->
