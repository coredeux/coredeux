# Reference

<!-- docs-nav-start -->
[Previous: Spring Boot Starter](/coredeux-drl-spring-boot-starter) | [Documentation Home](/) | [Next: Coredeux Import](/coredeux-import-overview)
<!-- docs-nav-end -->

This page is the quick reference for the DRL runtime.

## Main Contracts

- `DRLService`
- `DRLSourceResolver`
- `DRLCache`
- `RuleContext`
- `CoredeuxComponentRegistry`

## Default Behavior

- rule execution happens by `ruleId`
- compiled DRL is cached as a `KieBase`
- each call creates a new `KieSession`
- the runtime exposes `componentRegistry` as a global
- the native default compiler settings are `NATIVE` and `19`

## Common Properties

Classpath rule lookup:

```properties
coredeux.drl.classpath-prefix=rules/
coredeux.drl.classpath-suffix=.drl
```

Compiler overrides:

```properties
coredeux.drl.java-compiler=NATIVE
coredeux.drl.java-language-level=19
```

## Rule Shape

Typical DRL files declare the registry global:

```drl
global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;
```

The `then` block can then resolve approved Coredeux components on demand.

## Update Strategy

There is no automatic checksum/version scheme in the current design.

When a rule changes, purge the cache explicitly:

- `purgeCache(ruleId)`
- `purgeCache()`

<!-- docs-nav-start -->
[Previous: Spring Boot Starter](/coredeux-drl-spring-boot-starter) | [Documentation Home](/) | [Next: Coredeux Import](/coredeux-import-overview)
<!-- docs-nav-end -->
