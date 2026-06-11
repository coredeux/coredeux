# Import Value Handlers With DRL

<!-- docs-nav-start -->
[Previous: Coredeux DRL Custom Handlers](/coredeux-drl-custom-handlers) | [Documentation Home](/) | [Next: Export Value Handlers With DRL](/coredeux-drl-export-value-handlers)
<!-- docs-nav-end -->

This page shows how to author an import value handler as DRL-backed logic
instead of a Java bean.

The key idea is the same import column contract you already know from native
handlers, but the implementation body is moved into DRL.

## When To Use This Pattern

Use a DRL-backed import value handler when you want:

- the import column contract to stay the same
- the conversion logic to be replaceable without a rebuild
- the handler body to live in a rule source that can be converted at runtime
- the same handler invocation model across native and Spring hosts

## Runtime Path

When the import pipeline sees a handler name such as `demoUriImportHandler.drl`:

1. the shared value-handler service receives the handler name and the
   `ImportValueContext`
2. the DRL-aware service detects the `.drl` suffix
3. the service creates a `RuleContext`
4. the original `ImportValueContext` is placed in the `RuleContext.params`
   map under the key `context`
5. the DRL runtime executes the handler source
6. the rule writes the converted value into `RuleContext.output`

The context is passed through as data. It is not converted into a fact and it
is not flattened into separate session facts.

## The Java-Like Source Shape

The DRL devtools converter expects a Java-like source class with annotations.
The method name should line up with the DRL `RuleContext.method` value used by
the runtime service, which is `handle`.

Example source:

```java
package com.coredeux.demo.drl.imports;

import java.net.URI;

import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.impex.handler.ImportValueContext;

@DrlDefinition("demoUriImportHandler.drl")
public class DemoUriImportHandlerSource {

    @DrlRule(name = "handle", when = "$context : RuleContext(method == 'handle')")
    public void handle(RuleContext<Object> $context) {
        ImportValueContext context = (ImportValueContext) $context.getParams().get("context");
        String value = context.getEffectiveValue();

        if (value == null || value.isBlank()) {
            $context.setOutput(null);
            return;
        }

        URI uri = URI.create(value.trim());
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException(
                    "URI value must be absolute for column: " + context.getColumn().getName());
        }

        $context.setOutput(uri);
    }
}
```

The important part is the cast:

```java
ImportValueContext context = (ImportValueContext) $context.getParams().get("context");
```

That is how the DRL body gets back to the same payload that a native import
handler would have received.

## Resulting DRL Shape

The converter strips the authoring shell and keeps the rule body in DRL form.
The generated rule is conceptually similar to this:

```drl
package com.coredeux.demo.drl.imports

import java.net.URI

import com.coredeux.drl.model.RuleContext
import com.coredeux.impex.handler.ImportValueContext

rule "handle"
when
    $context : RuleContext(method == 'handle')
then
    ImportValueContext context = (ImportValueContext) $context.getParams().get("context");
    String value = context.getEffectiveValue();

    if (value == null || value.isBlank()) {
        $context.setOutput(null);
        return;
    }

    URI uri = URI.create(value.trim());
    if (!uri.isAbsolute()) {
        throw new IllegalArgumentException(
                "URI value must be absolute for column: " + context.getColumn().getName());
    }

    $context.setOutput(uri);
end
```

## How To Reference It From Import Configuration

Use the `.drl` suffix in the import column handler name:

```json
{
  "name": "documentationUrl",
  "handler": "demoUriImportHandler.drl"
}
```

The import engine does not need to know whether the handler is native or DRL.
It only asks the shared value-handler service to invoke the named handler.

## Practical Notes

- Keep the DRL body focused on conversion and validation.
- Use `ImportValueContext` to read the current column metadata, row value, and
  expected target type.
- Keep reusable helper logic in a separate DRL rule source or in a normal Java
  service if it must stay reusable across handlers.
- Use `CoredeuxImportException` for column-aware failures when you want the
  import logs to point to the failing column.

If you also need the export side, read the next page.

<!-- docs-nav-start -->
[Previous: Coredeux DRL Custom Handlers](/coredeux-drl-custom-handlers) | [Documentation Home](/) | [Next: Export Value Handlers With DRL](/coredeux-drl-export-value-handlers)
<!-- docs-nav-end -->
