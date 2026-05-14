# Coredeux Java Native Demo

This module shows how to run Coredeux without Spring by booting a plain Java
runtime and an embedded HTTP server.

The active runtime loads `META-INF/coredeux.yml`, which points at
`coredeux-entities.yml`. That file is now the single entity-definition source
for the native demo and includes the full set of sample entities, data access
services, hooks, validators, audit handlers, workflows, and backend examples.

Run the native demo from the repository root:

```bash
mvn -pl examples/coredeux-java-native-demo -am test
```

Primary application entrypoint:

- `com.coredeux.examples.nativejava.CoredeuxNativeDemoApplication`

That entrypoint starts the embedded HTTP server used by the native demo.

Additional focused sample programs are still available for the Postgres
walkthrough:

- `com.coredeux.examples.nativejava.postgres.PostgresCustomerMain`
- `com.coredeux.examples.nativejava.postgres.PostgresCustomerImportMain`

These samples walk through the lower-level Postgres-native Coredeux flow for
definition loading, create/load/update/search/remove, and import parsing.

The native demo expects a PostgreSQL database and reads optional connection
settings from:

```bash
COREDEUX_POSTGRES_URL=jdbc:postgresql://localhost:5432/coredeux
COREDEUX_POSTGRES_USER=coredeux
COREDEUX_POSTGRES_PASSWORD=coredeux
```
