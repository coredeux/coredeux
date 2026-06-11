# Export Value Handlers With DRL

<!-- docs-nav-start -->
[Previous: Import Value Handlers With DRL](/coredeux-drl-import-value-handlers) | [Documentation Home](/) | [Next: Coredeux DRL DevTools](/coredeux-drl-devtools)
<!-- docs-nav-end -->

This page shows how to author an export value handler as DRL-backed logic.

The native export pipeline still uses `CoredeuxExportValueHandler` and
`ExportValueContext`. The DRL path keeps the same context object but lets the
implementation live in a rule source instead of a Java bean.

## When To Use This Pattern

Use a DRL-backed export value handler when you want:

- the field contract to remain stable
- the formatting logic to be replaceable at runtime
- a rule-based implementation for redaction, formatting, or transformation
- the same `.drl` naming convention as the import side

## Runtime Path

When the export pipeline sees a handler name such as `dateFormatExportHandler.drl`:

1. the shared value-handler service receives the handler name and the
   `ExportValueContext`
2. the DRL-aware service detects the `.drl` suffix
3. the service creates a `RuleContext`
4. the original `ExportValueContext` is placed in the `RuleContext.params`
   map under the key `context`
5. the DRL runtime executes the handler source
6. the rule writes the converted value into `RuleContext.output`

The context is passed through as data, not as a fact.

## The Java-Like Source Shape

Example source for a date formatting export handler:

```java
package com.coredeux.demo.drl.exports;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.export.handler.ExportValueContext;

@DrlDefinition("dateFormatExportHandler.drl")
public class DateFormatExportHandlerSource {

    @DrlRule(name = "handle", when = "$context : RuleContext(method == 'handle')")
    public void handle(RuleContext<Object> $context) {
        ExportValueContext context = (ExportValueContext) $context.getParams().get("context");
        Object value = context.getResolvedValue();

        if (value == null) {
            $context.setOutput("");
            return;
        }

        if (!(value instanceof Date)) {
            throw new IllegalArgumentException("dateFormatExportHandler supports java.util.Date values only");
        }

        Date date = (Date) value;

        Map<String, Object> metadata = context.getField() == null ? Map.of() : context.getField().getMetadata();
        String pattern = String.valueOf(metadata.get("dateFormat"));
        ZoneId zone = ZoneId.of(String.valueOf(metadata.getOrDefault("timezone", "UTC")));

        String formatted = DateTimeFormatter.ofPattern(pattern).withZone(zone).format(date.toInstant());
        $context.setOutput(formatted);
    }
}
```

Again, the important line is the cast back to the export payload:

```java
ExportValueContext context = (ExportValueContext) $context.getParams().get("context");
```

## Resulting DRL Shape

The generated DRL keeps the rule logic and strips the Java shell:

```drl
package com.coredeux.demo.drl.exports

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Map

import com.coredeux.drl.model.RuleContext
import com.coredeux.export.handler.ExportValueContext

rule "handle"
when
    $context : RuleContext(method == 'handle')
then
    ExportValueContext context = (ExportValueContext) $context.getParams().get("context");
    Object value = context.getResolvedValue();

    if (value == null) {
        $context.setOutput("");
        return;
    }

    if (!(value instanceof Date)) {
        throw new IllegalArgumentException("dateFormatExportHandler supports java.util.Date values only");
    }

    Date date = (Date) value;

    Map<String, Object> metadata = context.getField() == null ? Map.of() : context.getField().getMetadata();
    String pattern = String.valueOf(metadata.get("dateFormat"));
    ZoneId zone = ZoneId.of(String.valueOf(metadata.getOrDefault("timezone", "UTC")));

    String formatted = DateTimeFormatter.ofPattern(pattern).withZone(zone).format(date.toInstant());
    $context.setOutput(formatted);
end
```

## How To Reference It From Export Configuration

Use the `.drl` suffix in the export field handler name:

```json
{
  "path": "profile:legacySignupDate",
  "handler": "dateFormatExportHandler.drl",
  "metadata": {
    "dateFormat": "dd/MM/yyyy",
    "timezone": "UTC"
  }
}
```

The export engine still sees only a handler name and a context object. The DRL
starter decides whether that handler is native or rule-backed.

## Practical Notes

- Keep the conversion rule small and deterministic.
- Read field metadata from `ExportValueContext.getField().getMetadata()`.
- Return strings, formatted text, or transformed values through
  `RuleContext.setOutput(...)`.
- Use the native handler when the logic is simple and does not need runtime
  replacement.

If you need the import-side equivalent, go back to the previous page.

<!-- docs-nav-start -->
[Previous: Import Value Handlers With DRL](/coredeux-drl-import-value-handlers) | [Documentation Home](/) | [Next: Coredeux DRL DevTools](/coredeux-drl-devtools)
<!-- docs-nav-end -->
