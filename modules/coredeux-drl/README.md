# coredeux-drl

`coredeux-drl` provides the plain Java dynamic runtime logic layer for
Coredeux.

It resolves DRL by rule id, compiles the rule set once, caches the compiled
`KieBase`, and executes it against a mutable `RuleContext` plus any extra
facts you supply.

The module stays Spring-free. Native applications can wire the resolver and the
component registry directly, while Spring Boot applications should use the
`coredeux-drl-spring-boot-starter`.

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

## Who Should Depend On This Module

Depend on `coredeux-drl` if your application or module:

- needs runtime-loaded DRL execution
- wants to compile and cache rule sets by unique identifier
- wants a plain Java DRL engine without Spring dependencies
- wants to reuse Coredeux component registry access from rule code
