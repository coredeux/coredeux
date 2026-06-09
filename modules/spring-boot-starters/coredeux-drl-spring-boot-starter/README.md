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

If you do not provide a language-level override, the runtime maps the current
Java version to the best supported DRL level:

- Java 17 -> DRL 17
- Java 18 -> DRL 17
- Java 19 -> DRL 19
- Java 20 through Java 25 -> DRL 19

The value `19` remains the highest verified DRL level and the default ceiling
for environments that support it, but Java 17 and 18 still fall back to `17`
so the framework stays usable on the minimum supported runtime.

Spring Boot applications should include the Coredeux core starter as well so
the same application has both the Coredeux runtime and the DRL bridge.
