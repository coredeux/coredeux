# Reference

<!-- docs-nav-start -->
[Previous: Spring Boot Starter](/coredeux-drl-spring-boot-starter) | [Documentation Home](/) | [Next: Coredeux DRL DevTools](/coredeux-drl-devtools)
<!-- docs-nav-end -->

This page is the quick reference for the DRL runtime.

## Runtime At A Glance

```text
ruleId -> DRLSourceResolver -> DRL text -> compile -> cache -> execute
```

The important thing to remember is that the runtime is not tied to one
storage system. The resolver abstraction lets you map a rule id to DRL from a
classpath file, a database row, a remote store, or any other source you choose.

## Main Contracts

- `DRLService`
- `DRLSourceResolver`
- `DRLCache`
- `RuleContext`
- `CoredeuxComponentRegistry`

## DRLService API

The runtime service has a small surface area on purpose:

```java
void execute(String ruleId, RuleContext context);
void execute(String ruleId, String source, RuleContext context);
void executeSource(String source, RuleContext context);
void purgeCache();
void purgeCache(String ruleId);
boolean isCached(String ruleId);
```

Use `execute(ruleId, context)` when the rule only needs the context.

Use `execute(ruleId, source, context)` when you already have the DRL text and
want to compile it, store the compiled version in cache, and execute it under a
known rule id.

Use `executeSource(source, context)` when you want to compile and run a source
string immediately without resolver lookup or cache storage.

Use `isCached(ruleId)` when you want to know whether the compiled DRL is already
in memory.

Use `purgeCache(ruleId)` after updating one rule source.

Use `purgeCache()` when you want a full refresh.

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

In Spring Boot applications, the starter can read the same keys from
`application.properties` or `application.yml` and apply them before runtime
bootstrap.

## Rule Shape

Typical DRL files declare the registry global:

```drl
global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;
```

The `then` block can then resolve approved Coredeux components on demand.

## RuleContext Contract

`RuleContext` is the object rules read from and write to during execution.

It carries:

- `method`: the method name or rule selection key
- `params`: arbitrary named inputs
- `facts`: additional objects inserted into the rule session
- `output`: the rule result
- `exception`: a captured exception from the rule
- `firedRules`: how many rules ran for the execution

Convenience helpers:

```java
RuleContext context = RuleContext.method("check")
        .param("entity", entity)
        .fact(entity);
```

After execution, read the result back from the same object:

```java
Object output = context.getOutput();
int firedRules = context.getFiredRules();
Exception exception = context.getException();
```

This design keeps the API mutable by reference, which makes it easy to use in
validation hooks and command-style operations.

The rule session facts come from `RuleContext.getFacts()`, so the calling code
can decide what gets inserted without changing the service signature.

You can use that same shape anywhere you execute a rule:

```java
RuleContext context = RuleContext.method("check")
        .param("entity", entity)
        .fact(entity);
drlService.execute("sample-rule", context);
```

To compile and cache caller-provided source under a known rule id:

```java
RuleContext context = RuleContext.method("check")
        .fact(entity);
drlService.execute("sample-rule", """
        rule "sample-rule"
        when
            $context : RuleContext(method == "check")
        then
            $context.setOutput("ok");
        end
        """, context);
```

To compile and run source immediately without cache or resolver:

```java
RuleContext context = RuleContext.method("check")
        .fact(entity);
drlService.executeSource("""
        rule "check"
        when
            $context : RuleContext(method == "check")
        then
            $context.setOutput("ok");
        end
        """, context);
```

## Component Registry

`CoredeuxComponentRegistry` is the bridge between rule logic and application
components.

The contract is intentionally small:

```java
<T> T getComponent(String name, Class<T> type);
```

Rules use it like this:

```java
SampleService sampleService = componentRegistry.getComponent("sampleService", SampleService.class);
```

The registry serves the same purpose as direct application-context access, but
without coupling the native DRL module to Spring.

Native behavior:

- `DefaultDRLService(DRLSourceResolver)` uses an empty
  `InMemoryCoredeuxComponentRegistry`.
- Native hosts can pass their own `InMemoryCoredeuxComponentRegistry`.
- Components are registered explicitly by name.
- Missing names and wrong types fail fast with a Coredeux exception.

Spring Boot behavior:

- the `coredeux-drl-spring-boot-starter` wires
  `SpringCoredeuxComponentRegistry`
- it resolves beans from the Spring `ApplicationContext`
- rules can access Spring services by bean name and expected type
- the DRL module itself remains Spring-free

This gives rules useful flexibility:

- the same generated DRL can run in native or Spring hosts
- tests can provide small fake services through the in-memory registry
- production Spring apps can expose real beans through the Spring registry
- rule code asks for contracts, not container internals
- applications control what rule code can resolve by choosing the registry
  contents or registry implementation

## End-To-End Example

```java
DRLSourceResolver sourceResolver = new ClasspathDRLSourceResolver();
CoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder()
        .component("sampleService", new SampleService())
        .build();
DRLService drlService = new DefaultDRLService(sourceResolver, registry);

RuleContext context = RuleContext.method("check")
        .param("entity", entity)
        .fact(entity);
drlService.execute("sample-rule", context);
```

That short flow is the heart of the runtime:

1. identify the rule by id
2. resolve the stored DRL
3. compile and cache it
4. execute it with context and facts from the same `RuleContext`
5. read the output back from the same `RuleContext`

The Java-to-DRL converter and its annotations live in
`coredeux-drl-devtools`.

## Threading Notes

- the `DRLService` instance can be reused across calls
- a new `KieSession` is created for every execution
- the compiled cache is concurrent
- `RuleContext` is mutable and should be treated as one request object
- do not share the same `RuleContext` across concurrent executions
- if multiple workers may update rule source, decide in advance who calls
  `purgeCache(...)`

## Related Tooling

If you are authoring annotated Java-like rule source, see
`coredeux-drl-devtools`.

## Update Strategy

There is no automatic checksum/version scheme in the current design.

When a rule changes, purge the cache explicitly:

- `purgeCache(ruleId)`
- `purgeCache()`

Good practice:

- purge a single rule when one source changes
- purge everything when many rules were reloaded together
- call `isCached(ruleId)` if you want to assert that compilation has already
  happened

<!-- docs-nav-start -->
[Previous: Spring Boot Starter](/coredeux-drl-spring-boot-starter) | [Documentation Home](/) | [Next: Coredeux DRL DevTools](/coredeux-drl-devtools)
<!-- docs-nav-end -->
