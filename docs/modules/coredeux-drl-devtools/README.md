# Coredeux DRL DevTools

<!-- docs-nav-start -->
[Previous: Coredeux DRL Reference](/coredeux-drl-reference) | [Documentation Home](/) | [Next: Coredeux Import](/coredeux-import-overview)
<!-- docs-nav-end -->

`coredeux-drl-devtools` is the authoring-time companion to the DRL runtime.

It is used when you want to write rules as annotated Java-like source, render
them into DRL, and publish the result to an external store. It does not execute
rules at runtime.

## What It Provides

- `JavaToDrlConverter`
- `AnnotationBasedJavaToDrlConverter`
- `ConvertedDrl`
- `DrlConversionException`
- `@DrlDefinition`
- `@DrlGlobal`
- `@DrlRule`

## How It Fits

Use this module when your workflow looks like this:

```text
annotated Java-like source -> convert -> store DRL -> execute through coredeux-drl
```

The runtime module remains focused on execution, caching, and component
registry access. This module stays focused on conversion and authoring support.

## Java First, DRL Second

When you document or author a rule source, show the full Java-like source first
and the generated DRL immediately after it.

That ordering helps a developer or agent understand the intended workflow:

1. write the full Java source in the IDE
2. implement every method required by the contract
3. use DevTools to generate DRL from that source
4. store the generated DRL externally
5. execute the DRL later through `coredeux-drl`

The examples below follow that pattern on purpose. They are not compressed
fragments.

## Quick Conversion Flow

The typical flow is:

1. read the annotated Java source as text
2. pass that text into `JavaToDrlConverter`
3. capture the `ruleId` and generated DRL text
4. store the DRL in your external source
5. execute it later through `coredeux-drl`

Example:

```java
JavaToDrlConverter converter = new AnnotationBasedJavaToDrlConverter();
ConvertedDrl converted = converter.convert(sourceText);

String ruleId = converted.getRuleId();
String drl = converted.getDrl();
```

If the source lives in a file, the converter still expects a string:

```java
import java.nio.file.Files;
import java.nio.file.Path;

String sourceText = Files.readString(Path.of("SampleRuleSource.java"));
ConvertedDrl converted = converter.convert(sourceText);
```

The converter does not execute rules. It only turns annotated source into DRL
that you can publish or store elsewhere.

## Example Source

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
    public void check(RuleContext<String> $context) {
        SampleService sampleService = componentRegistry.getComponent("sampleService", SampleService.class);
        $context.setOutput(sampleService.message());
        $context.setMessage("Check rule completed successfully.");
    }
}
```

The annotations are the conversion markers. They are present in the Java source
so DevTools can discover the rule source id, globals, and individual rule
methods before it strips the annotation layer and emits DRL.

## Generated DRL

The converter keeps the runtime imports and turns the annotated class into a
plain DRL rule file:

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
   $context.setMessage("Check rule completed successfully.");
end
```

## How It Renders

- `@DrlDefinition` becomes the rule source id.
- `@DrlGlobal` fields become DRL `global` declarations.
- `@DrlRule` methods become DRL `rule` blocks.
- The `when` attribute is copied into the DRL `when` clause.
- The method body is copied into the DRL `then` block.
- Converter annotation imports are removed from the generated DRL.
- Other imports are preserved.

The converter supports:

- one `@DrlDefinition` class per source file
- multiple `@DrlRule` methods in that class
- multiple `@DrlGlobal` fields

It does not try to understand your business semantics. If the annotated source
contains syntax that Drools does not like later, the generated DRL can still
fail at runtime or during a later compile step.

Prefer single quotes inside `when` conditions:

```java
@DrlRule(name = "check", when = "$context : RuleContext(method == 'check')")
```

That avoids escaping double quotes inside Java annotation strings.

## Publishing Pattern

Most teams will use this module in one of two ways:

- build-time conversion, where generated DRL is written to a folder or artifact
- runtime publishing, where generated DRL is stored in a database or object
  store and later loaded by `coredeux-drl`

In both cases, the generated `ruleId` should be the identifier used by the
runtime resolver.

## Authoring Rules

Do:

- show the complete source class first and the generated DRL second
- keep every method in the contract visible in the example
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

## What The Converter Does Not Do

- it does not execute the generated DRL
- it does not manage cache invalidation
- it does not decide where the rule text should be stored
- it does not wire Spring beans or runtime dependencies
- it does not guarantee that newer Java syntax will survive Drools parsing

That separation is intentional: devtools helps you author and publish, while
`coredeux-drl` handles execution.

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

## What Is Not Supported

The current DRL workflow does not support these things reliably:

- Java 16 `instanceof` pattern matching inside rule consequences
- Java 16 local records declared inside a consequence
- Java 21 record patterns
- Java 21 pattern matching `switch`
- assuming a Drools language level of `21` or `25` will unlock newer syntax
- using the converter as if it were the runtime engine
- relying on Java source compilation alone as proof that the generated DRL will
  compile
- sharing one `RuleContext` across concurrent executions
- direct Spring usage inside the native runtime module
- automatic cache refresh when external DRL source changes

In practice, the safest mental model is:

- author with simple Java-like syntax
- generate DRL
- inspect the output
- publish it externally
- run it through `coredeux-drl`

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

## Debugging Tips

- print the generated DRL before publishing it
- keep one small rule class per source file while you are learning the model
- start with simple Java syntax, then expand gradually
- if a rule fails later in Drools, compare the generated output with the
  original source instead of debugging the annotation layer first

## Typical Use Cases

- standalone publishing tools
- build-time conversion steps
- migration utilities for legacy rule sources
- admin or CLI tools for rule authors

## Next Step

If you want the runtime story, read the DRL module pages:

- [Coredeux DRL](/coredeux-drl)
- [DRL Overview](/coredeux-drl-overview)
- [Native Runtime](/coredeux-drl-native-runtime)
- [Spring Boot Starter](/coredeux-drl-spring-boot-starter)
- [DRL Reference](/coredeux-drl-reference)

<!-- docs-nav-start -->
[Previous: Coredeux DRL Reference](/coredeux-drl-reference) | [Documentation Home](/) | [Next: Coredeux Import](/coredeux-import-overview)
<!-- docs-nav-end -->
