# Coredeux DRL Spring Boot Starter

This module is the Spring Boot bridge for `coredeux-drl`.

`coredeux-drl` stays plain Java. The starter contributes Spring Boot
auto-configuration that creates the DRL runtime beans and lets a Spring
application use its existing bean graph from inside rules.

The starter wires:

- `ClasspathDRLSourceResolver`
- `DRLService`
- `SpringCoredeuxComponentRegistry` through the existing Coredeux core starter
- environment-driven overrides for the classpath DRL location
- environment-driven overrides for the Drools compiler language settings

Default classpath rule location:

```properties
coredeux.drl.classpath-prefix=rules/
coredeux.drl.classpath-suffix=.drl
```

Compiler defaults can be overridden before the DRL engine boots:

```properties
coredeux.drl.java-compiler=NATIVE
coredeux.drl.java-language-level=19
```

Spring Boot applications should include the Coredeux core starter as well so
the same application has both the Coredeux runtime and the DRL bridge.
