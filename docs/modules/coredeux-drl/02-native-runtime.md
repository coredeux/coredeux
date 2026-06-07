# Native Runtime

<!-- docs-nav-start -->
[Previous: Overview](/coredeux-drl-overview) | [Documentation Home](/) | [Next: Spring Boot Starter](/coredeux-drl-spring-boot-starter)
<!-- docs-nav-end -->

The native runtime is the plain Java way to use `coredeux-drl`.

It is the right choice when:

- you are not running inside Spring Boot
- you want to wire the DRL runtime yourself
- you want the smallest possible dependency surface

## Typical Wiring

In a native application you usually provide:

- a `DRLSourceResolver`
- a `CoredeuxComponentRegistry`
- optionally a `DRLCache`

The default native component registry is `InMemoryCoredeuxComponentRegistry`.

Example shape:

```java
DRLSourceResolver sourceResolver = new ClasspathDRLSourceResolver();
CoredeuxComponentRegistry componentRegistry = new InMemoryCoredeuxComponentRegistry(Map.of(
    "sampleService", new SampleService()
));

DRLService drlService = new DefaultDRLService(sourceResolver, componentRegistry);
```

The service resolves the DRL by rule id, compiles it once, and reuses the
cached compiled `KieBase` on later calls.

## Compiler Defaults

The native module defaults to:

- `drools.dialect.java.compiler=NATIVE`
- `drools.dialect.java.compiler.lnglevel=19`

If you want to override those settings in a native host, set the corresponding
system properties before the DRL service is first loaded.

## Cache Control

The runtime keeps compiled rule sets in memory and exposes explicit purge
operations:

- `purgeCache()`
- `purgeCache(ruleId)`

That keeps update behavior simple: replace the DRL source, then clear the
compiled cache entry.

<!-- docs-nav-start -->
[Previous: Overview](/coredeux-drl-overview) | [Documentation Home](/) | [Next: Spring Boot Starter](/coredeux-drl-spring-boot-starter)
<!-- docs-nav-end -->
