# Export Request And Output

<!-- docs-nav-start -->
[Previous: Export Overview](/modules/coredeux-export/01-overview) | [Documentation Home](/) | [Next: Export Runtime And Configuration](/modules/coredeux-export/03-runtime-and-configuration)
<!-- docs-nav-end -->

This page covers the request model, output formats, and the value shaping
features that make export useful in real applications.

## Request Model

The export request is made of four main pieces:

- `entity`
- `fieldList`
- `searchParams` or `query`
- `options`

### Java Example

```java
ExportRequest request = ExportRequest.builder()
        .entity(Customer.class.getName())
        .fieldList(List.of(
                ExportField.builder().path("name").build(),
                ExportField.builder().path("email").build(),
                ExportField.builder().path("active").build()))
        .searchParams(List.of(new SearchParams("active", "EQUALS", true)))
        .options(ExportOptions.builder()
                .format(ExportFormat.TEXT)
                .includeHeader(true)
                .build())
        .build();
```

### Raw JSON Example

```json
{
  "entity": "com.example.Customer",
  "fieldList": [
    { "path": "name" },
    { "path": "email" },
    { "path": "active" }
  ],
  "searchParams": [
    {
      "field": "active",
      "comparator": "EQUALS",
      "value": true
    }
  ],
  "options": {
    "format": "TEXT",
    "includeHeader": true
  }
}
```

## Field List

`ExportField` is intentionally small:

- `path`
- `handler`
- `metadata`

Example:

```json
{
  "fieldList": [
    { "path": "name" },
    {
      "path": "email",
      "handler": "maskEmailExportHandler",
      "metadata": {
        "visiblePrefix": 2
      }
    },
    {
      "path": "profile:legacySignupDate",
      "handler": "dateFormatExportHandler",
      "metadata": {
        "dateFormat": "dd/MM/yyyy",
        "timezone": "UTC"
      }
    }
  ]
}
```

Field path rules used by the code:

- `name` reads a root field.
- `profile:biography` walks into a nested object.
- `preferences:locale` reads a map entry.
- `roles:code` collects values from a collection of objects.
- only one collection hop is supported in one path.

The parser rejects nested collection paths because a single cell cannot
represent that shape cleanly.

## Query Strategies

The module supports two mutually exclusive ways to choose rows:

- `searchParams`
- `query`

The export service rejects requests that supply both.

### Search Params

`searchParams` works with the structured Coredeux comparator model:

```json
{
  "searchParams": [
    {
      "field": "status",
      "comparator": "EQUALS",
      "value": "ACTIVE"
    },
    {
      "field": "email",
      "comparator": "ANYWHERE",
      "value": "@example.com"
    }
  ]
}
```

What the data-access layer can support depends on the selected implementation.
For example, the current JPA implementation supports comparators such as
`EQUALS`, `NOTEQUALS`, `STARTSWITH`, `ANYWHERE`, `LESSTHAN`, `GREATERTHAN`,
`ISNULL`, `ISNOTNULL`, `ISEMPTY`, `ISNOTEMPTY`, `CONTAINS`, and `NOTCONTAINS`.

### Query

`query` lets the backend execute its own query string:

```json
{
  "query": {
    "text": "select c from Customer c where c.active = :active",
    "params": {
      "active": true
    }
  }
}
```

When no query or search params are supplied, export falls back to
`CoredeuxService.loadAll(...)`.

## Output Formats

### TEXT

Text output uses `|` by default.

```text
name|email|active
Jane|jane@example.com|true
```

You can change the separator:

```json
{
  "options": {
    "format": "TEXT",
    "textSeparator": ";"
  }
}
```

### XLSX

Excel output writes the same rows to an `.xlsx` workbook:

```json
{
  "options": {
    "format": "XLSX",
    "fileName": "customers.xlsx"
  }
}
```

The writer creates one sheet named `export`.

## Field Handlers

Every field can pass through an export value handler.
When no handler is set, Coredeux uses `defaultCoredeuxExportValueHandler`.
The handler receives an `ExportValueContext`, which is a small data payload and
not a service container. The `handler` field can point to either a native bean
name or a `.drl` handler name when the DRL starter is active.

Example handler:

```java
@Component("dateFormatExportHandler")
public class DateFormatExportHandler implements CoredeuxExportValueHandler {

    @Override
    public Object handle(ExportValueContext context) {
        Object value = context.getResolvedValue();
        if (value == null) {
            return "";
        }
        String pattern = String.valueOf(context.getField().getMetadata().get("dateFormat"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.withZone(ZoneOffset.UTC).format(((Date) value).toInstant());
    }
}
```

The default handler already knows how to deal with:

- `null`
- `BigDecimal`
- `Date`
- Java time values
- enums
- everything else through `String.valueOf(...)`

### DRL-backed Export Value Handlers

The same handler slot can also be backed by DRL when the DRL starter is on the
classpath.

In that mode:

- native bean names still resolve through the Java pipeline
- handler names ending in `.drl` are routed through the DRL runtime
- the DRL rule receives the same `ExportValueContext` payload

Use the DRL-specific guide when you want the export formatting logic authored
as a rule source instead of a Java bean.

## Java Usage

The response you get back is useful immediately:

```java
ExportResponse queued = exportService.queueExport(request);
ExportResponse current = exportService.getExport(queued.getUid());

if (ExportStatus.COMPLETED.equals(current.getStatus())) {
    ExportStorageArtifact storage = current.getStorage();
    System.out.println(storage.getCanonicalUrl());
}
```

## Defaults

The current code defaults to:

- `format`: `TEXT`
- `batchSize`: `100`
- `includeHeader`: `true`
- `textSeparator`: `|`
- `collectionSeparator`: `, `
- `fileName`: `coredeux-export.txt` or `coredeux-export.xlsx`

The next page shows where those defaults come from in the runtime wiring.

<!-- docs-nav-start -->
[Previous: Export Overview](/modules/coredeux-export/01-overview) | [Documentation Home](/) | [Next: Export Runtime And Configuration](/modules/coredeux-export/03-runtime-and-configuration)
<!-- docs-nav-end -->
