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

## A Realistic Call Flow

A native host usually does three things:

1. create a resolver that knows where the DRL lives
2. register the application components the rules are allowed to use
3. execute the rule by id with a `RuleContext`

Example:

```java
DRLSourceResolver sourceResolver = new ClasspathDRLSourceResolver(
        Thread.currentThread().getContextClassLoader(),
        "rules/",
        ".drl");

CoredeuxComponentRegistry componentRegistry = InMemoryCoredeuxComponentRegistry.builder()
        .component("sampleService", new SampleService())
        .build();

DRLService drlService = new DefaultDRLService(sourceResolver, componentRegistry);

RuleContext context = RuleContext.method("check")
        .param("entity", sampleEntity)
        .fact(sampleEntity);
drlService.execute("sample-rule", context);

Object output = context.getOutput();
int firedRules = context.getFiredRules();
```

When the rule throws, the runtime stores the exception on the context and then
raises an `IllegalStateException` so the caller can handle it in one place.

## Rule Context Contract

`RuleContext` is the object you pass into the runtime and read from after the
rule runs.

It carries:

- the current method name
- arbitrary `params`
- the output value
- a human-readable `message`
- a captured exception
- the number of fired rules

That makes it a good fit for request/response style hooks and validation rules
where the caller wants a mutable result object instead of a new return type.

## Working With Facts

Put additional session facts on the `RuleContext` with `.fact(...)` before you
call `execute(...)`.

If you want to compile and run a source string immediately without resolver
lookup or cache storage, use `executeSource(...)`:

```java
RuleContext context = RuleContext.method("check")
        .fact(sampleEntity);
drlService.executeSource("""
        rule "check"
        when
            $context : RuleContext(method == "check")
        then
            $context.setOutput("ok");
            $context.setMessage("Rule executed successfully.");
        end
        """, context);
```

Typed facts are easiest to work with:

```drl
when
   $entity : SampleEntity()
then
   $context.setOutput($entity.getName());
end
```

If the fact is bound as `Object`, cast it before using subtype methods:

```drl
when
   $entity : Object()
then
   SampleEntity sampleEntity = (SampleEntity) $entity;
   $context.setOutput(sampleEntity.getName());
end
```

## Compiler Defaults

The native module defaults to:

- `drools.dialect.java.compiler=NATIVE`
- `drools.dialect.java.compiler.lnglevel=19`

If you want to override those settings in a native host, set the corresponding
system properties before the DRL service is first loaded.

If you need to verify whether a rule is already compiled, call
`isCached(ruleId)`. If you update external DRL text, call
`purgeCache(ruleId)` for a single rule or `purgeCache()` for a full refresh.

## Cache Control

The runtime keeps compiled rule sets in memory and exposes explicit purge
operations:

- `purgeCache()`
- `purgeCache(ruleId)`

That keeps update behavior simple: replace the DRL source, then clear the
compiled cache entry.

This is the right model when the source is external and may change while the
application is running. The runtime does not try to guess whether the source
changed. You decide when the compiled version should be discarded.

<!-- docs-nav-start -->
[Previous: Overview](/coredeux-drl-overview) | [Documentation Home](/) | [Next: Spring Boot Starter](/coredeux-drl-spring-boot-starter)
<!-- docs-nav-end -->
