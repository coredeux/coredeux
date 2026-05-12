# Coredeux Java Native Examples

This module shows how to use `coredeux-core`, `coredeux-core-jpa`, and
`coredeux-import` from plain Java code, without Spring.

The runtime loads `META-INF/coredeux.yml`, which in turn points at
`coredeux-postgres-entities.yml`. That keeps framework configuration separate
from entity definitions, just like `persistence.xml` in JPA.

The examples then register the Postgres JPA data access service plus sample
validators/hooks/audit handlers in a `CoredeuxComponentRegistry`, and call the
Coredeux services directly.

Run the examples from the repository root:

```bash
mvn -pl examples/coredeux-java-native -am test
```

Main programs:

- `com.coredeux.examples.nativejava.postgres.PostgresCustomerMain`
- `com.coredeux.examples.nativejava.postgres.PostgresCustomerImportMain`

`PostgresCustomerMain` walks through definition loading, create, load, update,
search, and remove. It shows validation inside the create/update timeline, where
blank names are rejected before valid data is written.

`PostgresCustomerImportMain` reads `samples/postgres-customers.import`, parses it
with the parser now bundled into `coredeux-import`, wires the import service in
plain Java, and upserts customers through the same Coredeux service path. The
sample includes one invalid blank-name row so you can see row-level import
errors without stopping valid rows.

Both programs run configured validators, hooks, and audit handlers through normal
Coredeux service operations. They use JPA/Hibernate directly against PostgreSQL,
without Spring. They expect a PostgreSQL database and read optional connection
settings from:

```bash
COREDEUX_POSTGRES_URL=jdbc:postgresql://localhost:5432/coredeux
COREDEUX_POSTGRES_USER=coredeux
COREDEUX_POSTGRES_PASSWORD=coredeux
```
