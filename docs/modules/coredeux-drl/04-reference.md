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
- `JavaToDrlConverter`
- `AnnotationBasedJavaToDrlConverter`
- `@DrlDefinition`
- `@DrlGlobal`
- `@DrlRule`

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

## Java-To-DRL Converter

Use the converter when you want to author rule source as Java-like code and
generate DRL from it.

```java
package com.example.rules;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlGlobal;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.model.RuleContext;
import com.example.SampleService;

@DrlDefinition("sampleRuleSource")
public class SampleRuleSource {

    @DrlGlobal
    public CoredeuxComponentRegistry componentRegistry;

    @DrlRule(name = "check", when = "$context : RuleContext(method == 'check')")
    public void check(RuleContext $context) {
        SampleService sampleService = componentRegistry.getComponent("sampleService", SampleService.class);
        $context.setOutput(sampleService.message());
    }
}
```

The generated DRL will look like this:

```drl
import java.lang.*;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.drl.model.RuleContext;
import com.example.SampleService;

global CoredeuxComponentRegistry componentRegistry;

rule "check"
when
   $context : RuleContext(method == 'check')
then
   SampleService sampleService = componentRegistry.getComponent("sampleService", SampleService.class);
   $context.setOutput(sampleService.message());
end
```

Rendering behavior:

- `@DrlDefinition` sets the rule id.
- `@DrlGlobal` fields become DRL `global` declarations.
- `@DrlRule` methods become DRL `rule` blocks.
- The `when` attribute is copied as the DRL `when` condition.
- The method body is copied into the DRL `then` block.
- Converter annotation imports are removed from the generated DRL.
- Other imports are preserved.

Prefer single quotes inside `when` conditions:

```java
@DrlRule(name = "check", when = "$context : RuleContext(method == 'check')")
```

That avoids escaping double quotes inside Java annotation strings.

## Authoring Rules

Do:

- keep each rule method body self-contained
- import every runtime type used by the rule
- use typed Drools fact bindings when you need subtype methods
- cast values read from `RuleContext.params`
- inspect or log generated DRL when debugging
- purge the cache after updating external rule source

Avoid:

- relying on helper methods in the Java source class unless they are real
  imported runtime methods
- treating Java compile success as Drools compile success
- using `Object()` fact patterns without casts
- hiding complex rule selection inside one very large rule
- using Java syntax newer than the configured Drools language level

## Drools Java Dialect Limits

The converter does not replace Drools compilation. It only produces DRL text.
The generated `then` block is still compiled by the Drools Java dialect.

Observed in the Java compatibility POC:

- Java 5 generics, enhanced `for`, enums, and classic `switch` worked.
- Java 7 diamond, try-with-resources, and multi-catch worked.
- Java 8 lambdas, method references, streams, and functional interfaces worked.
- Java 10 `var` worked.
- Java 14 switch expressions worked.
- Java 15 text blocks worked at language level 19.
- Java 21 library APIs worked when the host JDK supplied the API; this is API
  usage, not Java 21 syntax.
- Java 16 pattern matching for `instanceof` failed in Drools declaration
  analysis.
- Java 16 local records inside the consequence failed.
- Java 21 record patterns failed.
- Java 21 pattern switch over sealed/record types failed.
- Asking the tested Drools version for language level `21` or `25` fell back to
  Java 11 detection, so it did not enable newer syntax.

Practical recommendation: keep rule consequences close to Java 8-15 style,
prefer explicit types and casts, and verify generated DRL with tests before
publishing it to an external source.

## Fact Typing

Typed fact patterns are safest:

```drl
when
   $psu : Psu()
then
   $context.setOutput($psu.getSkuCode());
end
```

If a fact is bound as `Object`, cast it first:

```drl
when
   $fact : Object()
then
   Psu psu = (Psu) $fact;
   $context.setOutput(psu.getSkuCode());
end
```

Values from `RuleContext.params` also need casts because map lookup returns
`Object`:

```drl
Psu psu = (Psu) $context.getParams().get("entity");
```

## Update Strategy

There is no automatic checksum/version scheme in the current design.

When a rule changes, purge the cache explicitly:

- `purgeCache(ruleId)`
- `purgeCache()`

<!-- docs-nav-start -->
[Previous: Spring Boot Starter](/coredeux-drl-spring-boot-starter) | [Documentation Home](/) | [Next: Coredeux Import](/coredeux-import-overview)
<!-- docs-nav-end -->
