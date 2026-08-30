# Reference

<!-- docs-nav-start -->
[Previous: Spring Boot Starter](/modules/coredeux-drl/03-spring-boot-starter) | [Documentation Home](/) | [Next: Coredeux DRL DevTools](/modules/coredeux-drl-devtools/)
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
<T> void execute(String ruleId, RuleContext<T> context);
<T> void execute(String ruleId, String source, RuleContext<T> context);
<T> void executeSource(String source, RuleContext<T> context);
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

All execution methods require a non-null `RuleContext` with a non-blank
`method`. The runtime stores `firedRules`, output, messages, and captured
exceptions on that object.

Coredeux DRL execution uses the callable method contract. The method name on
the context selects the rule to execute for handlers, hooks, validators, audit
handlers, and DRL data-access implementations.

For each execution:

- `context.method` must be non-blank
- exactly one rule must fire
- `firedRules == 0` means no rule matched the requested method
- `firedRules > 1` means the DRL has duplicate or ambiguous handlers for the
  same method

Use `isCached(ruleId)` when you want to know whether the compiled DRL is already
in memory.

Use `purgeCache(ruleId)` after updating one rule source.

When a stored rule source is deleted, purge the matching runtime cache entry so
the next request cannot execute stale compiled rules.

Use `purgeCache()` when you want a full refresh.

## Default Behavior

- rule execution happens by `ruleId`
- compiled DRL is cached as a bundle that stores the `KieBase` plus whether
  `componentRegistry` is required
- each call creates a new `KieSession`
- the runtime exposes `componentRegistry` as a global
- the native default compiler settings are `NATIVE` and `19`
- if no override is provided, Coredeux chooses the DRL language level from the running Java version

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

## Java Version To DRL Level Map

When Coredeux resolves the DRL language level automatically, it uses the
current Java feature version and picks the best supported Drools level below
it. The current map is intentionally small because the verified DRL language
levels are still `17` and `19`.

| Java version | Selected DRL level | Notes |
| --- | --- | --- |
| 17 | 17 | Minimum supported runtime floor |
| 18 | 17 | Falls back to the nearest supported level |
| 19 | 19 | Matches the current default DRL level |
| 20-25 | 19 | Stays on the highest verified DRL level |

If you provide `coredeux.drl.java-language-level`, that explicit override wins.
Use this only when you know the target runtime and the rule source you are
loading can compile with that exact Drools setting.

## Rule Shape

Typical DRL files declare the registry global:

```drl
global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;
```

The `then` block can then resolve approved Coredeux components on demand.

## RuleContext Contract

`RuleContext<T>` is the object rules read from and write to during execution.

It carries:

- `method`: the method name or rule selection key
- `params`: arbitrary named inputs
- `facts`: additional objects inserted into the rule session
- `output`: the rule result
- `message`: a human-readable explanation for tools or agents
- `exception`: a captured exception from the rule
- `firedRules`: how many rules ran for the execution

Convenience helpers:

```java
RuleContext<String> context = RuleContext.method("check")
        .param("entity", entity)
        .fact(entity);
```

`RuleContext.method("check")` is the preferred way to create an execution
context. The method must match exactly one rule condition.

After execution, read the result back from the same object:

```java
String output = context.getOutput();
String message = context.getMessage();
int firedRules = context.getFiredRules();
Exception exception = context.getException();
```

This design keeps the API mutable by reference, which makes it easy to use in
validation hooks and command-style operations.

The rule session facts come from `RuleContext.getFacts()`, so the calling code
can decide what gets inserted without changing the service signature.

Keep the converted rule body self-contained. Helper methods in the Java
authoring class are not preserved as reusable DRL methods after conversion, so
shared behavior should live in another DRL source or an external component
resolved through the registry.

You can use that same shape anywhere you execute a rule:

```java
RuleContext<String> context = RuleContext.method("check")
        .param("entity", entity)
        .fact(entity);
drlService.execute("sample-rule", context);
```

To compile and cache caller-provided source under a known rule id:

```java
RuleContext<String> context = RuleContext.method("check")
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
RuleContext<String> context = RuleContext.method("check")
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

RuleContext<String> context = RuleContext.method("check")
        .param("entity", entity)
        .fact(entity);
drlService.execute("sample-rule", context);
```

That short flow is the heart of the runtime:

1. identify the rule by id
2. resolve the stored DRL
3. compile and cache it
4. execute it with context and facts from the same `RuleContext<T>`
5. read the output back from the same `RuleContext<T>`

The Java-to-DRL converter and its annotations live in
`coredeux-drl-devtools`.

## Threading Notes

- the `DRLService` instance can be reused across calls
- a new `KieSession` is created for every execution
- the compiled cache is concurrent
- `RuleContext<T>` is mutable and should be treated as one request object
- do not share the same `RuleContext<T>` across concurrent executions
- if multiple workers may update rule source, decide in advance who calls
  `purgeCache(...)`

## Related Tooling

If you are authoring annotated Java-like rule source, see
`coredeux-drl-devtools`.

## Practical Guides

If you want the full examples instead of the reference summary, use the
dedicated pages:

- [Data Access With DRL](/modules/coredeux-drl/05-data-access)
- [Validators With DRL](/modules/coredeux-drl/06-validators)
- [Hooks With DRL](/modules/coredeux-drl/07-hooks)
- [Audit With DRL](/modules/coredeux-drl/08-audit)
- [Custom Handlers](/modules/coredeux-drl/09-custom-handlers)

Each of those guides shows the Java source first and the generated DRL right
after it, so the authoring path stays obvious for both developers and agents.

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
[Previous: Spring Boot Starter](/modules/coredeux-drl/03-spring-boot-starter) | [Documentation Home](/) | [Next: Coredeux DRL DevTools](/modules/coredeux-drl-devtools/)
<!-- docs-nav-end -->
