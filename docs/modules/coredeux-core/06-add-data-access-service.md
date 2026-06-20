# Add Or Choose A Data Access Service

<!-- docs-nav-start -->
[Previous: Module System](/modules/coredeux-core/05-modules) | [Documentation Home](/) | [Next: Available Data Access Implementations](/modules/coredeux-core/07-available-data-access-implementations)
<!-- docs-nav-end -->

Every Coredeux entity needs a `CoredeuxDataAccessService` that knows how to
store and load it.

This page helps you decide whether you can use one of the built-in services or
whether your application needs a custom adapter.

If an entity omits `storage.data-access-service`, Coredeux falls back to the
global `coredeux.data-access-service` value from `application.yml` or
`META-INF/coredeux.yml`. The same entity-level definition can also supply a
storage-level `identifier`, which is used only when the entity itself does not
declare one.

## The Basic Decision

Start with two questions:

1. Is your application Spring Boot based or plain Java native?
2. Is the entity stored in a SQL database or a NoSQL store?

That is usually enough to narrow the choice:

- SQL + Spring Boot: use the existing Spring starters
- SQL + native Java: wire the existing core services directly in your runtime
- NoSQL + Spring Boot: use the matching Spring starter
- NoSQL + native Java: wire the matching core service directly in your runtime

If the built-in service already matches the entity's storage model and query
style, use it. If not, create a custom adapter.

## When To Reuse An Existing Service

Reuse an existing service when:

- the storage technology already matches
- the entity can use the standard comparator and query behavior
- the application does not need special transaction or query rules
- the bean name already fits the YAML `storage.data-access-service` value

Examples:

- use JPA for relational entities
- use PostgreSQL JPA when JSONB or PostgreSQL-specific query behavior matters
- use JDBC when you want direct SQL without JPA overhead
- use MongoDB for document-shaped entities
- use Elasticsearch for search-oriented document indexing
- use Redis for JSON-backed storage or lightweight persistence paths

## When To Create A Custom Adapter

Create a custom adapter when the application needs something that the built-in
services do not provide:

- application-specific SQL
- custom table or index naming
- special conversion rules
- a non-standard transactional boundary
- an external store that is not covered by the built-in modules
- a host-specific integration requirement

The custom adapter still implements `CoredeuxDataAccessService`. The rest of
Coredeux should not care whether the implementation came from a module or from
your application code.

Example:

```java
public class CustomerJdbcDataAccessService implements CoredeuxDataAccessService {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Customer> rowMapper;

    public CustomerJdbcDataAccessService(JdbcTemplate jdbcTemplate, RowMapper<Customer> rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    @Override
    public <T> T load(String id, Class<T> type) {
        if (!Customer.class.equals(type)) {
            throw new UnsupportedOperationException("Only Customer is supported");
        }
        return type.cast(jdbcTemplate.queryForObject(
                "select id, name, email from customers where id = ?",
                rowMapper,
                id));
    }

    @Override
    public <T> String save(T entity) {
        Customer customer = (Customer) entity;
        jdbcTemplate.update(
                "insert into customers(id, name, email) values (?, ?, ?)",
                customer.getId(), customer.getName(), customer.getEmail());
        return customer.getId();
    }

    @Override
    public <T> void update(T entity) {
        Customer customer = (Customer) entity;
        jdbcTemplate.update(
                "update customers set name = ?, email = ? where id = ?",
                customer.getName(), customer.getEmail(), customer.getId());
    }

    @Override
    public <T> void remove(T entity) {
        Customer customer = (Customer) entity;
        jdbcTemplate.update("delete from customers where id = ?", customer.getId());
    }

    @Override
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        // Build SQL from the structured search params and return a SearchResult.
        // The SQL details can be as simple or as advanced as your application needs.
        throw new UnsupportedOperationException("Implement the SQL search path for your entity");
    }

    @Override
    public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage) {
        throw new UnsupportedOperationException("Implement the query path for your entity");
    }
}
```

In Spring Boot, expose it as a bean:

```java
@Bean("customerJdbcDataAccessService")
CoredeuxDataAccessService customerJdbcDataAccessService(JdbcTemplate jdbcTemplate, RowMapper<Customer> rowMapper) {
    return new CustomerJdbcDataAccessService(jdbcTemplate, rowMapper);
}
```

In native Java, register the same adapter during bootstrap and point the YAML
`storage.data-access-service` to `customerJdbcDataAccessService`.

## Real Demo Examples

The Spring Boot demo uses the PostgreSQL JPA implementation directly:

```java
@Configuration(proxyBeanMethods = false)
public class DemoJpaConfiguration {

    @Bean(name = "postgresCoredeuxJpaDataAccessService")
    public PostgresCoredeuxJpaDataAccessService postgresCoredeuxJpaDataAccessService() {
        return new PostgresCoredeuxJpaDataAccessService();
    }
}
```

The matching entity YAML points at that bean name:

```yaml
coredeux:
  entities:
    - full-class-name: com.coredeux.demo.domain.Customer
      storage:
        data-access-service: postgresCoredeuxJpaDataAccessService
```

The native demo does the same thing inside `CoredeuxNativeRuntime` by creating
the adapter and registering it under the name the YAML expects.

## Spring Boot Applications

In Spring Boot applications, you usually expose the adapter as a bean.

The starter modules can create the standard services for you, or you can define
your own `@Bean` when you need a custom implementation.

If the adapter already exists in one of the Spring Boot starters, you do not
need to register it manually. Spring's bean creation and the starter
auto-configuration take care of that for you. Manual bean registration is only
needed when you are providing a custom adapter or overriding the starter
default.

Example:

```java
@Configuration
class CoredeuxJpaConfig {

    @Bean("postgresCoredeuxJpaDataAccessService")
    CoredeuxDataAccessService postgresCoredeuxJpaDataAccessService(EntityManager entityManager) {
        return new PostgresCoredeuxJpaDataAccessService(entityManager);
    }
}
```

The entity YAML then points to that bean name through:

```yaml
storage:
  data-access-service: postgresCoredeuxJpaDataAccessService
```

## Native Java Applications

In native Java applications, register the adapter yourself during bootstrap.

The common pattern is:

1. create the storage client or entity manager
2. construct the `CoredeuxDataAccessService`
3. register it in your native runtime or component registry
4. reference the same bean name in the YAML definition

That keeps the Coredeux contract the same even though there is no Spring
container involved.

## SQL Versus NoSQL

For SQL databases, choose one of the relational services:

- generic JPA
- PostgreSQL-specific JPA
- JDBC

For NoSQL stores, choose one of the document or key-value services:

- MongoDB
- Elasticsearch
- Redis

The choice should match the shape of the entity and the kind of query behavior
you need, not just the storage technology name.

## The Coredeux Contract

No matter where the service comes from, Coredeux only needs three things:

- a stable bean name
- an implementation of `CoredeuxDataAccessService`
- a matching `storage.data-access-service` entry in the entity YAML

If the entity YAML does not include that entry, the global fallback bean name
is used instead.

Identifier resolution is separate from service resolution:

- entity `identifier` wins first
- storage `identifier` is the default for that entity definition
- `coredeux.identifier` can supply a global fallback
- Coredeux does not infer an identifier from field names

The framework uses that link to route CRUD and query work to the right backend.

## What To Read Next

- [Available Data Access Implementations](/modules/coredeux-core/07-available-data-access-implementations)
- [Adding a New Entity](/modules/coredeux-core/11-add-new-entity)

<!-- docs-nav-start -->
[Previous: Module System](/modules/coredeux-core/05-modules) | [Documentation Home](/) | [Next: Available Data Access Implementations](/modules/coredeux-core/07-available-data-access-implementations)
<!-- docs-nav-end -->
