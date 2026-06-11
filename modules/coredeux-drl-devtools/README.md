# coredeux-drl-devtools

`coredeux-drl-devtools` provides the authoring-time tooling for Coredeux DRL.

It is not the runtime engine. Use this module when you want to write DRL as
annotated Java-like source, convert it to DRL, and publish the generated rule
text to a database, file, S3 bucket, or any other external source.

Detailed reference:

- [docs/modules/coredeux-drl-devtools/README.md](/C:/Data/Development/Coredeux/oss/coredeux/docs/modules/coredeux-drl-devtools/README.md)

## What This Module Provides

`coredeux-drl-devtools` provides:

- `JavaToDrlConverter`
- `AnnotationBasedJavaToDrlConverter`
- `ConvertedDrl`
- `DrlConversionException`
- `@DrlDefinition`
- `@DrlGlobal`
- `@DrlRule`

## What It Does

The converter reads a Java source file, finds the annotated rule class, and
renders DRL text from it.

The runtime module stays separate. `coredeux-drl` executes DRL, while this
module helps generate it before storage or publishing.

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

## Generated DRL

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

## Rendering Notes

- `@DrlDefinition` becomes the rule source id.
- `@DrlGlobal` fields become DRL globals.
- `@DrlRule` methods become DRL rules.
- The `when` value is copied into the `when` clause.
- The method body is copied into the `then` block.
- Annotation imports are removed from the generated DRL.
- Non-annotation imports are preserved.

The converter supports:

- one `@DrlDefinition` class per source file
- multiple `@DrlRule` methods in that class
- multiple `@DrlGlobal` fields

It does not try to understand your business semantics. If the annotated source
contains syntax that Drools does not like later, the generated DRL can still
fail at runtime or during a later compile step.

Prefer single quotes inside `when` conditions so the annotation string stays
easy to read.

## Publishing Pattern

Most teams will use this module in one of two ways:

- build-time conversion, where generated DRL is written to a folder or artifact
- runtime publishing, where generated DRL is stored in a database or object
  store and later loaded by `coredeux-drl`

In both cases, the generated `ruleId` should be the identifier used by the
runtime resolver.

## Authoring Notes

Do:

- keep rule methods self-contained
- import every runtime type used by the rule
- use typed fact patterns when you need subtype methods
- cast values read from `RuleContext.params`
- inspect the generated DRL during development
- purge the cache after updating stored rule text

Avoid:

- relying on local helper methods that are not imported runtime classes
- assuming Java compilation means Drools compilation will succeed
- using `Object()` fact bindings without casts
- putting all selection logic into one giant rule
- using Java syntax newer than the configured Drools language level

## What The Converter Does Not Do

- it does not execute the generated DRL
- it does not manage cache invalidation
- it does not decide where the rule text should be stored
- it does not wire Spring beans or runtime dependencies
- it does not guarantee that newer Java syntax will survive Drools parsing

That separation is intentional: devtools helps you author and publish, while
`coredeux-drl` handles execution.

## Known Drools Limits

The converter does not extend Drools itself. It only generates DRL text.
The `then` block still has to compile inside the Drools Java dialect.

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
- Asking the tested Drools version for language level `21` or `25` fell back
  to Java 11 detection, so it did not enable newer syntax.

Practical rule of thumb: keep rule consequences close to Java 8-15 style,
prefer explicit types and casts, and verify generated DRL with tests before
publishing it.

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

- a standalone publishing tool
- a build step that converts annotated source into stored DRL
- a migration utility for legacy marker-based rule sources
- a CLI or internal admin tool for rule authors

## Testing

Run the module tests with dependencies:

```powershell
mvn -pl modules/coredeux-drl-devtools -am test
```

## Who Should Depend On This Module

Depend on `coredeux-drl-devtools` if your application or tool:

- writes DRL from Java-like annotated source
- stores rule text externally before execution
- wants a converter without bringing the runtime engine into the same module
