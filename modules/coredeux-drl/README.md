# coredeux-drl

`coredeux-drl` provides the plain Java dynamic runtime logic layer for
Coredeux.

It resolves DRL by rule id, compiles the rule set once, caches the compiled
`KieBase`, and executes it against a mutable `RuleContext` plus any extra
facts you attach with `.fact(...)`. The context can also carry a human-readable
`message` for agent and tool integrations alongside the typed `output`.

The module stays Spring-free. Native applications can wire the resolver and the
component registry directly, while Spring Boot applications should use the
`coredeux-drl-spring-boot-starter`.

## Java Support

Coredeux supports Java 17 and above.

- minimum supported runtime/build floor: Java 17
- current project/runtime baseline: Java 21
- DRL language level is selected from the running Java version unless you
  override it in configuration
- the current verified DRL levels are `17` and `19`
- the compiled rule cache stores both the `KieBase` and the registry-global
  requirement together as one cached bundle
- deleting a stored rule should also purge the matching runtime cache entry

Coredeux targets Java 17+ at build time, runs the demos on Java 21, and
automatically chooses a Drools language level from the runtime Java version
unless you override it in configuration. The current map is:

- Java 17 -> DRL 17
- Java 18 -> DRL 17
- Java 19 -> DRL 19
- Java 20 through Java 25 -> DRL 19

Detailed reference:

- [docs/modules/coredeux-drl/04-reference.md](/C:/Data/Development/Coredeux/oss/coredeux/docs/modules/coredeux-drl/04-reference.md)

## What This Module Provides

`coredeux-drl` provides:

- `DRLService`
- `DRLSourceResolver`
- `DRLCache`
- `RuleContext`
- `DefaultDRLService`
- `ClasspathDRLSourceResolver`
- `InMemoryDRLCache`
- `DrlRuntimeBootstrap`

## Runtime Model

The runtime flow is:

`ruleId -> DRLSourceResolver -> DRL text -> compile -> cache -> execute`

The service injects a `CoredeuxComponentRegistry` global into the rule session
so DRL code can resolve approved platform components on demand.

Typical rule files live under the classpath `rules/` folder and use the
`componentRegistry` global:

```drl
global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;
```

## Component Registry Access

`CoredeuxComponentRegistry` is the safe application-component bridge exposed to
rules. It gives rule code a stable API for resolving a named component by the
contract it expects:

```java
SampleService sampleService = componentRegistry.getComponent("sampleService", SampleService.class);
```

That keeps rule code independent from the actual host container:

- in native/plain Java usage, `DefaultDRLService` can use
  `InMemoryCoredeuxComponentRegistry`
- in Spring Boot usage, the starter wires `SpringCoredeuxComponentRegistry`

`InMemoryCoredeuxComponentRegistry` is backed by a map of explicitly registered
objects. It is useful for non-Spring applications, tests, CLI tools, workers, or
any host that wants to decide exactly which components rules can access.

`SpringCoredeuxComponentRegistry` is backed by the Spring `ApplicationContext`.
It resolves beans by name and type, giving rules application-context access
without making `coredeux-drl` depend on Spring.

The registry gives flexibility in two directions: rules can call application
services when needed, and the application can swap the registry implementation
without changing the rule source. Missing components or type mismatches fail
fast instead of silently returning the wrong object.

For Java-to-DRL authoring and conversion tools, see
`coredeux-drl-devtools`.

## Native Usage

The plain Java path is intended for non-Spring hosts and test harnesses.

You provide:

- a `DRLSourceResolver` implementation
- a `CoredeuxComponentRegistry`
- an optional `DRLCache` implementation

The default component registry for the native path is
`InMemoryCoredeuxComponentRegistry`.

## Spring Usage

Spring Boot applications should use the starter module.

The Spring starter defaults to:

- `ClasspathDRLSourceResolver`
- `SpringCoredeuxComponentRegistry`
- environment-driven overrides for the classpath source location
- environment-driven compiler property overrides before DRL bootstrap

## Testing

Run the module tests with dependencies:

```powershell
mvn -pl modules/coredeux-drl -am test
```

## Drools Compatibility Notes

The runtime still has Drools Java dialect limitations. The converter and its
authoring-time annotations now live in `coredeux-drl-devtools`, but the rules
they generate must still compile inside Drools.

Observed with the current POC:

- Java 5 generics, enhanced `for`, enums, and classic `switch` work.
- Java 7 diamond, try-with-resources, and multi-catch work.
- Java 8 lambdas, method references, streams, and functional interfaces work.
- Java 10 `var` works.
- Java 14 switch expressions work.
- Java 15 text blocks work when the Drools language level is high enough.
- Java 21 library APIs can work when the application runs on a JDK that
  provides those APIs.
- Java 16 pattern matching for `instanceof` was rejected by Drools declaration
  analysis.
- Java 16 local records inside a rule consequence were rejected.
- Java 21 record patterns and pattern switch were rejected.
- Setting the language level to `21` or `25` did not unlock newer syntax in the
  tested Drools version; it fell back to Java 11 detection.

For fact typing:

- A typed fact pattern such as `$psu : Psu()` exposes `$psu` as `Psu` in
  `then`.
- An `Object()` pattern must be cast before subtype methods are called.
- Values read from `RuleContext.params` must be cast because `Map` returns
  `Object`.

## Who Should Depend On This Module

Depend on `coredeux-drl` if your application or module:

- needs runtime-loaded DRL execution
- wants to compile and cache rule sets by unique identifier
- wants a plain Java DRL engine without Spring dependencies
- wants to reuse Coredeux component registry access from rule code

For authoring-time conversion of annotated Java-like rule sources, use
`coredeux-drl-devtools`.
