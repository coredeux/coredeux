# Coredeux Core Spring Boot Starter

This module is the Spring Boot bridge for `coredeux-core`.

`coredeux-core` stays plain Java. The starter contributes Spring Boot
auto-configuration that creates the Coredeux infrastructure beans and lets a
Spring application provide its own domain beans.

Coredeux core now also ships with a map-backed `META-INF/coredeux.yml` loader
for native Java applications. Spring apps keep using `application.properties`
or `application.yml`, and the starter can merge those values into the same
property shape before wiring the framework.

The starter also understands a global fallback data access service through
`coredeux.data-access-service`. If an entity definition omits
`storage.data-access-service`, Coredeux uses that fallback bean name instead.

The starter currently wires:

- entity definition loading from `coredeux.entities.config-location`
- `EntityDefinitionRegistry`
- `CoredeuxComponentRegistry` backed by the Spring `ApplicationContext`
- default core module handlers
- `CoredeuxStrategy`
- `CoredeuxModuleService`
- `CoredeuxService`
- servlet request context resolution when Spring Web is on the classpath

Default entity definition location:

```properties
coredeux.entities.config-location=classpath:coredeux-entities.yml
```

Global fallback data access service:

```properties
coredeux.data-access-service=postgresCoredeuxJpaDataAccessService
```

Import now has its own Spring Boot starter module so the core starter stays
focused on the runtime, lifecycle, and request-context pieces. Export can follow
the same pattern later.
