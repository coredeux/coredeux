# coredeux-core-mongodb

`coredeux-core-mongodb` provides a MongoDB-backed implementation of
`CoredeuxDataAccessService`.

This module depends on `coredeux-core` and is intended for applications that
want to use MongoDB as the persistence adapter behind Coredeux.

Detailed reference:

- [docs/modules/core-mongodb/reference.md](/C:/Data/Development/Coredeux/coredeux-oss/coredeux/docs/modules/core-mongodb/reference.md)

## What This Module Provides

This module currently provides one Spring bean implementing
`CoredeuxDataAccessService`:

- `defaultCoredeuxMongoDataAccessService`
  MongoDB-backed CRUD, search, and query adapter

The adapter advertises its own comparator set through
`supportedComparators(Class<?> type)` so callers can adapt search requests to
the backing store.

## Why This Module Exists

MongoDB is a good fit when the entity model is document-oriented and you want a
non-relational persistence option that still plugs into the Coredeux SPI.

The module uses `MongoTemplate` rather than Spring Data repositories so it can
stay aligned with Coredeux's dynamic, entity-definition-driven model.

## Testing

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
