# Coredeux Raw JSON Import

<!-- docs-nav-start -->
[Previous: Coredeux Import Overview](01-overview.md) | [Documentation Home](../../README.md) | [Next: Text And Excel File Import](03-text-and-excel-file-import.md)
<!-- docs-nav-end -->

This guide teaches how to use `coredeux-import` directly with JSON.

Use this path when your application, API, workflow engine, migration tool, or
another parser already knows how to produce an `ImportRequest`. You do not need
the text import parser for this flow.

If you are working from an import file instead of JSON, jump to
[Text And Excel File Import](03-text-and-excel-file-import.md).

The raw JSON flow is:

```text
your JSON payload
  -> ImportRequest
  -> CoredeuxImportService.validateData(...)
  -> CoredeuxImportService.importData(...)
  -> CoredeuxService / configured data access backend
```

## First Import

The smallest useful request creates one product:

```json
{
  "statements": [
    {
      "operation": "CREATE",
      "entity": "com.example.Product",
      "columns": [
        { "name": "sku" },
        { "name": "name" },
        { "name": "active", "defaultValue": "true" }
      ],
      "rows": [
        {
          "values": {
            "sku": "P-100",
            "name": "Demo Product"
          }
        }
      ]
    }
  ]
}
```

This means:

- create a `com.example.Product`.
- write `sku`, `name`, and `active`.
- use `true` for `active` because the row omitted it.

Column names are target entity field names. Row values are keyed by column name.

## Validate Before Import

Use `validateData` when you want to check the request without writing data:

```java
ImportResponse response = importService.validateData(request);
```

Use `importData` to validate and then execute:

```java
ImportResponse response = importService.importData(request);
```

`importData` does not execute rows if validation fails.

## Request Options

Put execution options in `options`:

```json
{
  "options": {
    "passes": 2,
    "failFast": false,
    "validateOnly": false
  },
  "statements": []
}
```

Use `passes` when row-key references may be resolved by later rows or later
statements.

Use `failFast: true` when the first final-pass row error should stop the
import.

Use `validateOnly: true` when calling `importData` but still wanting dry-run
behavior.

## Operations

Supported operations:

- `CREATE`: always create a new entity.
- `UPSERT`: update if found, otherwise create.
- `MODIFY`: update an entity that must already exist.
- `DELETE`: remove an entity when found.
- `FETCH`: find an existing entity and store its row key for later references.

For every non-`CREATE` operation, use exactly one resolution strategy:

- unique columns
- lookup
- query

Coredeux rejects requests that mix strategies because the target row must be
identified through one clear path.

## Strategy 1: Unique Columns

Use `unique: true` when equality matching is enough:

```json
{
  "operation": "UPSERT",
  "entity": "com.example.Product",
  "columns": [
    { "name": "sku", "unique": true },
    { "name": "name" },
    { "name": "price" }
  ],
  "rows": [
    {
      "values": {
        "sku": "P-100",
        "name": "Demo Product",
        "price": "19.99"
      }
    }
  ]
}
```

This searches for an existing product where `sku EQUALS P-100`.

Use multiple unique columns for compound identity:

```json
{
  "columns": [
    { "name": "product", "reference": "sku", "unique": true },
    { "name": "currency", "unique": true },
    { "name": "amount" }
  ]
}
```

### Null Unique Search

Use `nullSearch: true` when null is intentionally part of identity:

```json
{
  "operation": "MODIFY",
  "entity": "com.example.Product",
  "columns": [
    { "name": "sku", "unique": true },
    { "name": "variant", "unique": true, "nullSearch": true },
    { "name": "name" }
  ],
  "rows": [
    {
      "values": {
        "sku": "P-100",
        "name": "Base Product"
      }
    }
  ]
}
```

The missing `variant` value searches with `ISNULL`.

## Strategy 2: Lookup

Use `lookup` when a structured search needs comparators or a different source
column.

```json
{
  "operation": "MODIFY",
  "entity": "com.example.Product",
  "lookup": [
    {
      "field": "sku",
      "comparator": "STARTSWITH"
    }
  ],
  "columns": [
    { "name": "sku" },
    { "name": "name" }
  ],
  "rows": [
    {
      "values": {
        "sku": "ABC-",
        "name": "Updated Name"
      }
    }
  ]
}
```

This searches target field `sku` using the value from import column `sku`.

Use `column` when the source column and target search field differ:

```json
{
  "operation": "MODIFY",
  "entity": "com.example.Customer",
  "lookup": [
    {
      "field": "emailHash",
      "column": "email",
      "comparator": "EQUALS"
    }
  ],
  "columns": [
    { "name": "email", "handler": "emailHashImportHandler" },
    { "name": "name" }
  ],
  "rows": [
    {
      "values": {
        "email": "john@example.com",
        "name": "John"
      }
    }
  ]
}
```

That searches target field `emailHash` using the value derived from import
column `email`. The handler can normalize and hash the email before lookup.

Comparator names depend on the configured data access implementation. Ask the
application through `CoredeuxService.supportedComparators(entityClass)`.

## Strategy 3: Query

Use `query` when the target backend must run a store-specific query.

```json
{
  "operation": "MODIFY",
  "entity": "com.example.Product",
  "query": {
    "text": "lower(sku) = :sku and status = :status",
    "params": {
      "sku": {},
      "status": {
        "column": "state"
      }
    }
  },
  "columns": [
    { "name": "sku", "handler": "lowercaseHandler" },
    { "name": "state" },
    { "name": "name" }
  ],
  "rows": [
    {
      "values": {
        "sku": "P-100",
        "state": "LIVE",
        "name": "Updated Product"
      }
    }
  ]
}
```

The import module derives:

```text
sku    = value from column sku
status = value from column state
```

Then it calls:

```java
coredeuxService.query(queryText, params, targetClass, -1, -1);
```

The import module does not parse or rewrite the query text. JPA may use
`:paramName`; another backend may use JSON, DSL, or a different parameter
style.

## References

Use `reference` when a value should become another entity.

Single-field reference:

```json
{
  "name": "category",
  "reference": "code"
}
```

If the row value is `fruit`, Coredeux searches the category entity by
`code EQUALS fruit`.

Compound reference:

```json
{
  "name": "catalogueVersion",
  "reference": "catalogue:version"
}
```

Row value:

```text
electronics:Online
```

Use `\:` when a value segment contains a literal colon.

Row-key reference:

```json
{
  "name": "customer",
  "reference": "*"
}
```

This resolves a value such as `&customer1` from an earlier row key.

## Row Keys

Rows can store internal references with `key`:

```json
{
  "operation": "CREATE",
  "entity": "com.example.Customer",
  "columns": [
    { "name": "email" },
    { "name": "name" }
  ],
  "rows": [
    {
      "key": "&customer1",
      "values": {
        "email": "john@example.com",
        "name": "John"
      }
    }
  ]
}
```

Later rows can use `reference: "*"` and value `&customer1`.

Row-key references are internal execution state. They are not returned in
`ImportResponse`.

## Collections

Primitive collection values are comma-separated strings:

```json
{
  "name": "tags",
  "mode": "replace"
}
```

```json
{
  "values": {
    "tags": "featured,summer"
  }
}
```

Use `\,` for a literal comma:

```json
{
  "values": {
    "tags": "red\\, blue,featured"
  }
}
```

Modes:

- `replace`: replace the collection.
- `append`: add incoming values to the current collection.
- `clear`: clear or initialize the collection.

## Maps

Map conversion is opt-in. Use `jsonMapImportHandler`:

```json
{
  "name": "attributes",
  "handler": "jsonMapImportHandler"
}
```

The row value can be a JSON object string:

```json
{
  "values": {
    "attributes": "{\"color\":\"blue\",\"size\":42}"
  }
}
```

Or it can already be a JSON object in the request:

```json
{
  "values": {
    "attributes": {
      "color": "blue",
      "size": 42
    }
  }
}
```

The handler supports flat maps with scalar values. Nested objects and arrays are
rejected with a column-level error.

## Macros And Defaults

Macros are simple string replacements:

```json
{
  "macros": {
    "&defaultStatus": {
      "value": "ACTIVE"
    }
  },
  "statements": [
    {
      "operation": "CREATE",
      "entity": "com.example.Customer",
      "columns": [
        { "name": "email" },
        { "name": "status", "defaultValue": "&defaultStatus" }
      ],
      "rows": [
        {
          "values": {
            "email": "john@example.com"
          }
        }
      ]
    }
  ]
}
```

The blank or missing `status` value becomes `ACTIVE`.

## Metadata

Many DTOs support `metadata`:

- macros
- statements
- columns
- lookup entries
- query
- query params
- rows

The import engine stores metadata and passes it to handlers, but it does not
interpret metadata as core behavior.

Example column metadata:

```json
{
  "name": "sku",
  "handler": "skuNormalizeHandler",
  "metadata": {
    "trim": true,
    "case": "upper",
    "prefix": "SHOP-"
  }
}
```

Use metadata for handler options, tracing, tenant/source hints, or
application-specific extensions.

### Example: java.util.Date With dateFormat

The default handler does not convert into `java.util.Date`. When a legacy model
still uses `Date`, implement a custom handler and pass a pattern through column
metadata.

Raw JSON example:

```json
{
  "statements": [
    {
      "operation": "UPSERT",
      "entity": "com.example.CustomerProfile",
      "columns": [
        { "name": "customer", "reference": "*", "unique": true },
        {
          "name": "legacySignupDate",
          "handler": "legacyDateImportHandler",
          "metadata": { "dateFormat": "dd/MM/yyyy" }
        }
      ],
      "rows": [
        { "values": { "customer": "&cust1", "legacySignupDate": "04/05/2026" } }
      ]
    }
  ]
}
```

This delegates conversion to `legacyDateImportHandler`, which reads
`metadata.dateFormat` and parses the string value into `java.util.Date`.

The full reference implementation is documented (with source code) in
[Import Reference](04-reference.md) under "legacyDateImportHandler (java.util.Date)".

## Complete Example

This example creates a product and then creates an owner that references the
product by row key:

```json
{
  "macros": {
    "&defaultCategory": {
      "value": "SOFTWARE"
    }
  },
  "options": {
    "passes": 2,
    "failFast": false
  },
  "statements": [
    {
      "operation": "CREATE",
      "entity": "com.example.Product",
      "columns": [
        { "name": "sku" },
        { "name": "price", "defaultValue": "10" },
        { "name": "active", "defaultValue": "true" },
        { "name": "category", "defaultValue": "&defaultCategory" },
        { "name": "tags" },
        { "name": "attributes", "handler": "jsonMapImportHandler" }
      ],
      "rows": [
        {
          "key": "&product1",
          "values": {
            "sku": "P-100",
            "tags": "primary\\, literal,featured",
            "attributes": "{\"color\":\"blue\",\"size\":42}"
          }
        }
      ]
    },
    {
      "operation": "CREATE",
      "entity": "com.example.ProductOwner",
      "columns": [
        { "name": "email" },
        { "name": "product", "reference": "*" }
      ],
      "rows": [
        {
          "values": {
            "email": "owner@example.com",
            "product": "&product1"
          }
        }
      ]
    }
  ]
}
```

## Common Failure Cases

Missing resolution strategy:

```json
{
  "operation": "MODIFY",
  "entity": "com.example.Product",
  "columns": [
    { "name": "sku" },
    { "name": "name" }
  ]
}
```

Coredeux rejects this because `MODIFY` needs unique columns, lookup, or query.

Mixed resolution strategies:

```json
{
  "operation": "MODIFY",
  "entity": "com.example.Product",
  "query": {
    "text": "sku = :sku",
    "params": {
      "sku": {}
    }
  },
  "columns": [
    { "name": "sku", "unique": true },
    { "name": "name" }
  ]
}
```

Coredeux rejects this because the statement mixes `query` and `unique`.

Bad handler name:

```json
{
  "name": "sku",
  "handler": "missingHandler"
}
```

The row fails because the handler cannot be resolved from Spring.

Bad enum value:

```json
{
  "values": {
    "category": "not-a-real-enum"
  }
}
```

Enum values must match Java enum constants exactly.

## Testing Your JSON

Recommended flow:

1. Deserialize JSON into `ImportRequest`.
2. Call `validateData`.
3. Fix all validation logs.
4. Call `importData`.
5. Treat response logs as user-facing diagnostics.

The module tests include raw JSON fixtures under:

```text
modules/coredeux-import/src/test/resources/samples
```

Those fixtures are deserialized and executed through `CoredeuxImportService`, so
they verify the JSON contract rather than only testing Java builders.

<!-- docs-nav-start -->
[Previous: Coredeux Import Overview](01-overview.md) | [Documentation Home](../../README.md) | [Next: Text And Excel File Import](03-text-and-excel-file-import.md)
<!-- docs-nav-end -->
