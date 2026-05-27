# Available Data Access Implementations

<!-- docs-nav-start -->
[Previous: Add Or Choose A Data Access Service](/06-add-data-access-service) | [Documentation Home](/) | [Next: Add a Hook](/08-add-hook)
<!-- docs-nav-end -->

This page lists the built-in `CoredeuxDataAccessService` implementations
available in the repository today.

The bean names are the important part of the contract. The same names can be
used by both Spring Boot apps and native Java apps, even though the wiring style
is different.

## How The Demo Uses Them

The Spring Boot demo points each entity at a concrete data access bean name in
`coredeux-entities.yml`:

```yaml
coredeux:
  entities:
    - full-class-name: com.coredeux.demo.domain.Customer
      storage:
        data-access-service: postgresCoredeuxJpaDataAccessService
    - full-class-name: com.coredeux.demo.domain.jdbc.JdbcInventoryItem
      storage:
        data-access-service: defaultCoredeuxJdbcDataAccessService
    - full-class-name: com.coredeux.demo.domain.mongodb.MongoAuditTrail
      storage:
        data-access-service: defaultCoredeuxMongoDataAccessService
    - full-class-name: com.coredeux.demo.domain.elasticsearch.ElasticsearchCatalogEntry
      storage:
        data-access-service: defaultCoredeuxElasticsearchDataAccessService
    - full-class-name: com.coredeux.demo.domain.redis.RedisSessionSnapshot
      storage:
        data-access-service: defaultCoredeuxRedisDataAccessService
```

The native demo registers the same bean names during bootstrap:

```java
InMemoryCoredeuxComponentRegistry components = InMemoryCoredeuxComponentRegistry.builder()
        .component("postgresCustomerDataAccess", customerDataAccess)
        .component("defaultCoredeuxJdbcDataAccessService", jdbcDataAccessService)
        .component("defaultCoredeuxMongoDataAccessService", mongoDataAccessService)
        .component("defaultCoredeuxElasticsearchDataAccessService", elasticsearchDataAccessService)
        .component("defaultCoredeuxRedisDataAccessService", redisDataAccessService)
        .build();
```

That is the Coredeux pattern in practice: the entity YAML chooses the bean
name, and the host application decides how that bean is created.

For Spring Boot applications, you normally do not write that registry code
yourself. The matching starter creates the bean and puts it into the Spring
context automatically. You only add explicit `@Bean` registration when you are
overriding the default adapter or supplying a custom implementation.

## SQL Implementations

| Module | Bean name | Typical use |
| --- | --- | --- |
| `coredeux-core-jpa` | `defaultCoredeuxJpaDataAccessService` | Generic relational persistence when standard JPA is enough |
| `coredeux-core-jpa` | `postgresCoredeuxJpaDataAccessService` | PostgreSQL-specific JPA with JSONB-aware behavior |
| `coredeux-core-jdbc` | `defaultCoredeuxJdbcDataAccessService` | Direct SQL/JDBC without JPA |

### Spring Boot SQL Starters

The matching Spring Boot starters live under `modules/spring-boot-starters`:

- `coredeux-core-jpa-spring-boot-starter`
- `coredeux-core-jdbc-spring-boot-starter`

These starters register the SQL adapters for Spring applications.

That means a Spring Boot app normally only needs the bean name in YAML. The
starter creates the bean, so you do not manually build the adapter registry the
way a native app does.

### Native SQL Usage

In a native app, you create the same service implementations yourself and
register them during bootstrap.

That is how the native demo wires:

- `defaultCoredeuxJpaDataAccessService`
- `postgresCoredeuxJpaDataAccessService`
- `defaultCoredeuxJdbcDataAccessService`

The native runtime builds the data-source or entity-manager objects first, then
wraps them in the matching Coredeux adapter, and finally puts those adapters
into the component registry.

## NoSQL Implementations

| Module | Bean name | Typical use |
| --- | --- | --- |
| `coredeux-core-mongodb` | `defaultCoredeuxMongoDataAccessService` | MongoDB-backed document persistence |
| `coredeux-core-elasticsearch` | `defaultCoredeuxElasticsearchDataAccessService` | Search-oriented document persistence |
| `coredeux-core-redis` | `defaultCoredeuxRedisDataAccessService` | Redis-backed JSON storage |

### Spring Boot NoSQL Starters

The matching Spring Boot starters live under `modules/spring-boot-starters`:

- `coredeux-core-mongodb-spring-boot-starter`
- `coredeux-core-elasticsearch-spring-boot-starter`
- `coredeux-core-redis-spring-boot-starter`

These starters register the NoSQL adapters for Spring applications.

Just like the SQL side, Spring Boot apps get the bean through auto-configuration
and only need custom `@Bean` methods when they are replacing the starter
default.

### Native NoSQL Usage

In a native app, you wire the same service classes directly in your runtime
bootstrap.

That is how the native demo keeps the bean names stable even without Spring.

The demo runtime creates the MongoDB, Elasticsearch, and Redis clients first,
then constructs the Coredeux adapters, and then registers them with the same
names used in the entity YAML.

## How To Pick One

Use the implementation that best matches your entity shape and query style:

- if you need relational transactions and normal SQL-style data, start with JPA
- if you need PostgreSQL-specific JSONB behavior, use the PostgreSQL JPA service
- if you want direct SQL, use JDBC
- if your entity is document-shaped, use MongoDB
- if the entity is search-oriented, use Elasticsearch
- if the entity fits a JSON-backed key-value style, use Redis

## Why The Bean Names Matter

Coredeux does not select a data access service by package or by module name.
It selects the service using the bean name configured in the entity YAML.

That means the same entity definition can move between:

- Spring Boot and native hosts
- SQL and NoSQL backends
- built-in and custom adapters

as long as the YAML points at the right bean name.

## What To Read Next

- [Add Or Choose A Data Access Service](/06-add-data-access-service)
- [Add a Hook](/08-add-hook)
- [Core JPA Reference](14-core-jpa-reference.md)
- [Core JDBC Reference](15-core-jdbc-reference.md)
- [Core Elasticsearch Reference](16-core-elasticsearch-reference.md)
- [Core MongoDB Reference](17-core-mongodb-reference.md)
- [Core Redis Reference](18-core-redis-reference.md)

<!-- docs-nav-start -->
[Previous: Add Or Choose A Data Access Service](/06-add-data-access-service) | [Documentation Home](/) | [Next: Add a Hook](/08-add-hook)
<!-- docs-nav-end -->
