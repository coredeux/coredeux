# Text And Excel File Import

<!-- docs-nav-start -->
[Previous: Coredeux Raw JSON Import](/modules/coredeux-import/02-raw-json-import) | [Documentation Home](/) | [Next: Coredeux Export Overview](/modules/coredeux-export/01-overview)
<!-- docs-nav-end -->

This guide teaches the Coredeux import file format from scratch.

If you want the JSON request form that the file parser ultimately produces,
see [Coredeux Raw JSON Import](/modules/coredeux-import/02-raw-json-import).

An import file is a readable table-shaped source that describes:

- which entity type to import
- which operation to run
- which fields to write
- how existing records are found
- how references, defaults, handlers, collections, and metadata should behave

The text parser reads pipe-separated text. The Excel parser reads workbook
cells. Both compile the same import language into an `ImportRequest`. The
`coredeux-import` module then validates and executes that request.

## Mental Model

Think of an import file as a set of small tables.

Each table starts with a statement header:

```text
UPSERT com.example.Product | sku(unique=true) | name | active
```

Every row below that header belongs to the statement:

```text
                            | P-100            | Demo Product | true
                            | P-200            | Other Product | false
```

Together:

```text
UPSERT com.example.Product | sku(unique=true) | name          | active
                            | P-100            | Demo Product  | true
                            | P-200            | Other Product | false
```

This means:

- target entity: `com.example.Product`
- operation: `UPSERT`
- fields: `sku`, `name`, `active`
- existing product lookup: `sku`
- rows: two products

Tip: Read each statement left to right: operation, entity, columns, rows.

## A Complete First File

```text
OPTIONS(passes=2,failFast=false)

&Category=com.example.Category
&Product=com.example.Product

UPSERT &Category | code(unique=true) | name
                 | fruit             | Fruit
                 | dairy             | Dairy

UPSERT &Product | sku(unique=true) | name   | category(reference=code) | active(default=true)
                | apple-1          | Apple  | fruit                    |
                | milk-1           | Milk   | dairy                    | false
```

This file:

- configures import execution with `OPTIONS`.
- defines aliases for long entity class names.
- upserts two categories by `code`.
- upserts two products by `sku`.
- resolves product `category` values by category `code`.
- defaults blank `active` values to `true`.

That is the core format. Everything else in this guide builds on this shape.

## Basic Syntax Rules

Use pipes to separate cells:

```text
UPSERT &Product | sku | name
                | P-1 | Demo
```

Whitespace around cells is ignored.

Blank lines are ignored.

Lines starting with `#` are comments:

```text
# Product seed data
UPSERT &Product | sku(unique=true) | name
```

Use backslash to escape separators inside values:

```text
CREATE &Product | sku  | name
                | A\|1 | Pipe In Name
```

Tip: Align pipes for readability, but alignment is not required.

## Text And Excel Sources

You can write the format as plain text or as an Excel workbook.

In text files, `|` separates cells:

```text
UPSERT &Product | sku(unique=true) | name
                | P-1              | Demo
```

In Excel files, each workbook cell is already a cell boundary:

| A | B | C |
|---|---|---|
| `UPSERT &Product` | `sku(unique=true)` | `name` |
|  | `P-1` | `Demo` |

The Excel parser reads one sheet and passes the resulting cells through the same
parser behavior used by text files. If no sheet name is provided, the first
workbook sheet is imported. If a sheet name is provided, only that sheet is
imported.

Programmatically this uses the shared parser contract:

```java
ImportRequest firstSheet = excelParser.parse(inputStream);
ImportRequest namedSheet = excelParser.parse(inputStream, "Products");
```

Important Excel rules:

- Use text cells and blank cells only.
- Do not use formulas.
- Do not rely on Excel number, date, boolean, or formula types.
- Use the optional sheet name when the workbook contains multiple import sheets.
- A blank first cell means the row has no row key.
- Multiline cell text is supported.
- A literal pipe inside a workbook cell is treated as data, not a boundary.

This keeps Excel imports equivalent to text imports: every cell is read as a
string, and the normal import flow handles conversion, handlers, references,
lookup, query, metadata, and execution.

## Request Options

Use `OPTIONS(...)` to configure the whole import.

```text
OPTIONS(passes=2,failFast=false,validateOnly=false)
```

Available options:

- `passes`: how many times Coredeux should process the statements.
- `failFast`: whether execution should stop after the first final-pass row error.
- `validateOnly`: whether execution should validate without applying changes.

Use `passes=2` when rows refer to other rows created earlier or later in the
same import.

Use `failFast=true` when a single row failure means the rest of the file should
not run.

Use `validateOnly=true` when checking a file before applying it.

Note: If multiple `OPTIONS(...)` lines set the same option, the later value
wins.

## Aliases

Use aliases to keep files readable.

```text
&Product=com.example.Product

UPSERT &Product | sku(unique=true) | name
                | P-1              | Demo
```

Without aliases, every statement would need the full class name:

```text
UPSERT com.example.Product | sku(unique=true) | name
```

Tip: Put aliases near the top of the file and keep alias names short.

## Operations

Every statement starts with an operation.

```text
UPSERT &Product | sku(unique=true) | name
```

Supported operations:

- `CREATE`: create a new entity.
- `UPSERT`: update if found, create if not found.
- `MODIFY`: update an entity that must already exist.
- `DELETE`: remove an entity when found.
- `FETCH`: find an entity and store its row key for later references.

Use `CREATE` when every row must become a new record.

Use `UPSERT` for repeatable configuration or seed data.

Use `MODIFY` when missing data should be treated as an error.

Use `DELETE` for cleanup files.

Use `FETCH` when later rows need to reference an existing record by row key.

Important: `UPSERT`, `MODIFY`, `DELETE`, and `FETCH` must define exactly one
existing-record strategy: `unique`, `lookup`, or `query`.

## Columns

Columns define the target fields and import behavior.

```text
UPSERT &Product | sku(unique=true) | name(default=Unnamed) | active(default=true)
```

Common column options:

- `unique=true`: use this column to find existing records.
- `nullSearch=true`: allow a unique lookup to search for null.
- `default=value`: use this value when the row cell is blank.
- `defaultValue=value`: same as `default`.
- `reference=field`: resolve the value as another entity.
- `handler=beanName`: use a custom value handler.
- `mode=replace|append|clear`: collection update mode.
- `metadata.key=value` or `meta.key=value`: custom metadata for handlers or tooling.

Example:

```text
UPSERT &Product | sku(unique=true) | name(default=Unnamed)
                | P-1              |
```

The product name becomes `Unnamed`.

Tip: Keep column names the same as entity field names unless a custom handler or
future mapping layer intentionally owns the difference.

## Finding Existing Records

For non-`CREATE` statements, Coredeux must find the existing record before it can
update, delete, or fetch it.

There are three strategies.

Use only one strategy per statement.

## Strategy 1: Unique Columns

Use `unique=true` when equality matching is enough.

```text
UPSERT &Product | sku(unique=true) | name
                | P-1              | Demo
```

This finds an existing product where `sku = P-1`.

Use multiple unique columns for compound identity:

```text
UPSERT &Price | product(reference=sku,unique=true) | currency(unique=true) | amount
              | P-1                                | AUD                   | 19.99
```

Use this when:

- the row contains the exact value needed to find the record.
- the target field and import column are the same concept.
- equality matching is sufficient.

This is the best default strategy.

### Null Unique Search

Use `nullSearch=true` when null is a meaningful part of identity.

```text
UPSERT &Product | sku(unique=true) | variant(unique=true,nullSearch=true) | name
                | P-1              |                                      | Base Product
```

This can search for a record where `sku = P-1` and `variant IS NULL`.

Note: Use `nullSearch` intentionally. Blank values can be mistakes, so avoid
making nullable identity casual.

## Strategy 2: Lookup

Use `lookup` when finding the record needs more than equality on the imported
field, but can still be expressed as field/comparator/value search predicates.

Example:

```text
MODIFY &Product(lookup=sku:sku:STARTSWITH:false)
               | sku | name
               | ABC | Prefix Match Product
```

Lookup shorthand:

```text
lookup=targetField[:sourceColumn[:comparator[:nullSearch]]]
```

Meaning:

- `targetField`: entity field to search.
- `sourceColumn`: import column used to derive the search value.
- `comparator`: search comparator, default `EQUALS`.
- `nullSearch`: whether null values should search with `ISNULL`.

Comparator names are not owned by the import parser. They are capabilities of
the data access service selected for the imported entity. For example, a JPA
data access service may support relational comparators such as `EQUALS`,
`STARTSWITH`, and `GREATERTHAN`, while another store may expose a smaller or
different set. Use the comparators advertised by
`CoredeuxService.supportedComparators(entityClass)` for the target application.

If the target field and source column have the same name, keep it short:

```text
MODIFY &Product(lookup=sku)
               | sku | name
               | P-1 | Demo
```

Use lookup when a handler derives the search value:

```text
MODIFY &Customer(lookup.field=emailHash,lookup.comparator=EQUALS)
                | emailHash(handler=emailHashHandler) | name
                | john@example.com                    | John
```

Here `emailHash` is a real field on `Customer`. The file provides a readable
email address in that column, and `emailHashHandler` converts it to the stored
hash value before lookup. The lookup searches the target field `emailHash`.

For multiple lookup predicates:

```text
MODIFY &Customer(lookup.1.field=emailHash,lookup.1.comparator=EQUALS,lookup.2.field=status,lookup.2.comparator=EQUALS)
                | emailHash(handler=emailHashHandler) | status | name
                | john@example.com                    | ACTIVE | John
```

If `lookup.<index>.comparator` is omitted, the parser defaults it to `EQUALS`.
The example keeps it explicit so the lookup intent is visible in the file.

Column-level lookup is also available:

```text
MODIFY &Customer | emailHash(lookup.field=emailHash,handler=emailHashHandler) | name
                 | john@example.com                                          | John
```

Use lookup when:

- you need a comparator such as `STARTSWITH`.
- the search target field differs from the import column.
- a handler derives the lookup value.
- the search can still be expressed as structured search predicates.

Tip: Prefer `lookup` over `query` when possible. It is easier to validate and
less tied to one database technology.

## Strategy 3: Query

Use `query` when finding the record requires a backend-specific query.

```text
MODIFY &Product(query="lower(sku) = :sku",query.params=sku)
               | sku(handler=lowercaseHandler) | name
               | P-1                           | Demo
```

The import engine passes `query` text and derived params to
`CoredeuxService.query`. It does not parse or rewrite the query text.

That means the query syntax belongs to the configured data access service.

For example, a JPA adapter may expect `:sku`. A MongoDB or search adapter may
expect a JSON or DSL expression.

Declare multiple query params like this:

```text
MODIFY &Product(query="lower(sku) = :sku and status = :status",query.params="sku,status:state")
               | sku(handler=lowercaseHandler) | state | name
               | P-1                           | LIVE  | Demo
```

`status:state` means query parameter `status` gets its value from import column
`state`.

You can also use explicit param syntax:

```text
MODIFY &Product(query="sku = :sku",query.param.sku=sku)
               | sku | name
               | P-1 | Demo
```

Use query when:

- structured lookup cannot express the search.
- the backend has query features you need.
- the search is naturally database-specific.

Note: Quote `query.params` when it contains commas:

```text
query.params="sku,status:state"
```

Tip: Use `query` as the escape hatch, not the default. It is powerful, but less
portable than `unique` and `lookup`.

## Choosing Unique, Lookup, Or Query

Use this order:

1. Start with `unique`.
2. Move to `lookup` if you need comparators, null search, or a different source
   column.
3. Move to `query` only when the data access backend must run a custom query.

Examples:

```text
# direct equality
UPSERT &Product | sku(unique=true) | name

# derived search value
MODIFY &Customer(lookup.field=emailHash,lookup.comparator=EQUALS)
                | emailHash(handler=emailHashHandler) | name

# backend-specific search
MODIFY &Product(query="lower(sku) = :sku",query.params=sku)
               | sku(handler=lowercaseHandler) | name
```

Important: Do not combine strategies in one statement.

Invalid:

```text
MODIFY &Product(query="sku = :sku",query.params=sku) | sku(unique=true) | name
```

This mixes `query` and `unique`.

## Header Lines

The normal and recommended shape is to keep the statement and column headers on
one logical row.

Text:

```text
UPSERT &Product | sku(unique=true) | name | price
                | P-1              | Demo | 19.99
```

Excel:

| A | B | C | D |
|---|---|---|---|
| `UPSERT &Product` | `sku(unique=true)` | `name` | `price` |
|  | `P-1` | `Demo` | `19.99` |

Use the same shape for Excel whenever possible. Excel already has real cell
boundaries, so splitting a statement row and a header row usually makes the
file harder to understand.

### Header Continuation

Text files may put columns on the next physical line when the statement options
are too long to read comfortably.

```text
MODIFY &Product(query="lower(sku) = :sku and status = :status",query.params="sku,status:state")
               | sku(handler=lowercaseHandler) | state | name
               | P-1                           | LIVE  | Demo
```

If the statement line has no columns, the next line starting with `|` becomes
the column header.

Use header continuation only for readability in long text statements, usually
with query or multi-lookup options.

Avoid using header continuation in Excel samples and normal workbooks. In Excel,
prefer one row for the statement plus headers:

| A | B | C | D |
|---|---|---|---|
| `MODIFY &Product(query="lower(sku) = :sku",query.params=sku)` | `sku(handler=lowercaseHandler)` | `name` | `price` |
|  | `p-1` | `Demo` | `19.99` |

Do not use header continuation to represent multiline values. Multiline values
belong in quoted row cells, explained later in this guide.

## References

Use `reference` when a value should resolve to another entity.

### Single-Field Reference

```text
UPSERT &Category | code(unique=true) | name
                 | fruit             | Fruit

UPSERT &Product | sku(unique=true) | name  | category(reference=code)
                | apple-1          | Apple | fruit
```

The product category is found by category `code`.

### Compound Reference

Use colon-separated reference fields for compound references.

```text
UPSERT &Product | sku(unique=true) | catalogueVersion(reference=catalogue:version)
                | apple-1          | electronics:Online
```

Use `\:` when a segment contains a literal colon.

```text
UPSERT &Product | sku(unique=true) | catalogueVersion(reference=catalogue:version)
                | apple-1          | electronics\:au:Online
```

### Row-Key Reference

Use row keys when a later row needs the entity created or fetched by an earlier
row.

```text
CREATE &Customer | email | name
&cust1           | john@example.com | John

CREATE &Address | customer(reference=*) | line1
                | &cust1                | 1 Demo Street
```

`reference=*` means the value is a row key.

Use row-key references when:

- generated identifiers are not known in the file.
- one statement creates data and another statement points to it.
- a `FETCH` statement loads an existing entity for later use.

Note: Row-key references are internal to import execution and are not returned
in the import response.

## Collection Columns

Collection values are comma-separated.

```text
UPSERT &User | email(unique=true) | roles(reference=code)
             | john@example.com   | admin,buyer
```

Use `\,` when a value contains a literal comma.

```text
UPSERT &Product | sku(unique=true) | tags
                | P-1              | red\, blue,featured
```

Collection modes:

- `mode=replace`: replace the existing collection.
- `mode=append`: add incoming values to the existing collection.
- `mode=clear`: clear or initialize the collection.

Example:

```text
MODIFY &User | email(unique=true) | roles(reference=code,mode=append)
             | john@example.com   | premium
```

Use `replace` when the file is the source of truth.

Use `append` when the file enriches existing data.

Use `clear` when the import should empty the collection.

## Custom Handlers

Use `handler` when a column needs custom conversion or custom lookup value
derivation.

```text
UPSERT &Product | sku(handler=skuNormalizeHandler,unique=true) | name
                | p-100                                       | Demo
```

Handlers are Spring beans.

Use handlers for:

- unsupported Java target types.
- normalization.
- derived lookup/query values.
- JSON object or map conversion.
- application-specific parsing.

Metadata can configure handlers:

```text
UPSERT &Product | sku(handler=skuNormalizeHandler,metadata.case=upper,metadata.trim=true) | name
                | p-100                                                                  | Demo
```

The handler can read the column metadata and apply those options.

Tip: If a type is not supported by the default handler, create a custom handler
instead of forcing generic object conversion into the default path.

### Example: java.util.Date With dateFormat

The default handler does not convert into `java.util.Date`. When a legacy model
still uses `Date`, implement a custom handler and pass a format through column
metadata.

Text import file example:

```text
UPSERT com.example.CustomerProfile
       | customer(reference=*,unique=true) | legacySignupDate(handler=legacyDateImportHandler,metadata.dateFormat=dd/MM/yyyy)
       | &cust1                            | 04/05/2026
```

This says:

- the target field is `legacySignupDate` (a `java.util.Date` field).
- `legacyDateImportHandler` parses the incoming value.
- the parsing pattern is provided via `metadata.dateFormat`.

## Metadata

Metadata is optional extension data.

It can be attached to:

- aliases
- statements
- columns
- lookup entries
- query
- query params
- rows

Use `metadata.` or `meta.`:

```text
UPSERT &Product(metadata.batch=base,metadata.priority=10) | sku(metadata.source=feed,metadata.enabled=true) | name
```

The stored statement metadata keys are `batch` and `priority`.
The stored `sku` column metadata keys are `source` and `enabled`.

Values are parsed as booleans or whole numbers when obvious:

```text
metadata.batch=base
metadata.source=feed
metadata.enabled=true
metadata.priority=10
```

Use metadata for:

- handler configuration.
- trace data such as sheet, source, package, tenant, or line number.
- parser hints.
- application extensions.

Do not use metadata for behavior that every import should understand. That
should become an explicit syntax field.

## Rows And Row Keys

Rows are values for the most recent statement.

```text
UPSERT &Product | sku(unique=true) | name
                | P-1              | Demo
&p2             | P-2              | Other
```

If a row has the same number of cells as columns, it has no row key.

If a row has one extra leading cell, that cell is the row key.

Row metadata goes in the row-key cell:

```text
&p2(sheet=Products,line=42) | P-2 | Other
```

Use row keys only when another row needs to reference the result.

## Escaping And Quoting

Escape literal pipes with `\|`.

```text
CREATE &Product | sku  | name
                | A\|1 | Pipe Value
```

Escape literal commas with `\,`.

```text
CREATE &Product | sku | tags
                | P-1 | red\, blue,featured
```

Escape literal colons with `\:` inside lookup, query-param, or compound
reference segments.

Quote option values that contain commas:

```text
query.params="sku,status:state"
```

Whole-cell quotes are stripped from row values:

```text
CREATE &Product | sku | name
                | P-1 | "Demo Product"
```

The `name` value is `Demo Product`.

Quoted row values may span multiple physical lines in text files:

```text
CREATE &Article | code | body
                | A-1  | "Line one
Line two
Line three"
```

The `body` value contains the newline characters between the quoted lines.
Pipes, commas, colons, and lines that start with `#` are treated as part of the
value while the quote is open.

Use this for genuinely large text values such as descriptions, biographies,
notes, templates, or article bodies. Do not use it to split headers.

In Excel, put the multiline text inside a single workbook cell. The row and
column shape does not change:

| A | B | C |
|---|---|---|
| `CREATE &Article` | `code` | `body` |
|  | `A-1` | `Line one`<br>`Line two`<br>`Line three` |

That Excel `body` cell is one cell containing newline characters. It is not
three import rows.

The parser rejects an import file when a quoted value is not closed before the
end of the file.

Tip: For complex nested data, prefer a custom handler or JSON import request
until richer parser formats are added.

## Parser Validation

The parser catches file-shape errors before execution.

Parser-level validation includes:

- source text must not be null.
- rows cannot appear before a statement.
- statements must declare an entity.
- statements must declare at least one column.
- column names must not be blank.
- duplicate column names are rejected.
- row cell counts must match the statement columns.
- option keys must not be blank.
- `passes` must be an integer.
- lookup shorthand must use `field[:column[:comparator[:nullSearch]]]`.
- query-param shorthand must use `paramName[:columnName]`.

Execution-time validation still belongs to `coredeux-import`. It validates target
fields, handlers, references, collection modes, and resolution strategies.
Comparator validity is ultimately enforced by the data access service that
executes the lookup.

## Practical Tips

Use aliases in real files.

Keep one entity type per statement.

Use blank lines between statement blocks.

Prefer `UPSERT` plus `unique` for repeatable seed data.

Prefer `lookup` over `query` when structured predicates are enough.

Use `query` only when the backend query language is required.

Use `reference` for entity relationships.

Use `reference=*` only when you need row-key references.

Quote option values that contain commas.

Escape literal pipes in row cells with `\|`.

Use metadata for handler configuration and traceability.

Put long query and lookup statements on two lines.

Use Excel when authors are more comfortable editing rows and cells in a
workbook. Keep the cells text-only so the parser sees exactly the same values a
plain text file would have provided.

## Current Scope

Implemented now:

- text-to-`ImportRequest`
- XLS/XLSX-to-`ImportRequest`
- pipe-separated cells
- workbook cells as import cells
- first-sheet default for Excel imports
- named-sheet selection for Excel imports
- request options
- aliases and alias metadata
- statement metadata
- row keys and row metadata
- statement query text
- statement query params
- query and query-param metadata
- column unique/default/reference/handler/mode options
- column metadata
- column-level lookup and query params
- statement-level lookup metadata
- indexed statement-level lookup predicates
- escaping for `|`, `,`, and `:`
- quoted option and row values
- continuation column header line after long statement options
- multiline row values in text and Excel sources

Not implemented yet:

- CSV dialects
- multipart handling at the parser layer itself; applications expose
  file-upload endpoints around the parser, as the demo does
- source-location tracking beyond parser error line numbers
- automatic execution without building an `ImportRequest` first

The parser should remain a compiler into `ImportRequest`, not a second import
engine.

<!-- docs-nav-start -->
[Previous: Coredeux Raw JSON Import](/modules/coredeux-import/02-raw-json-import) | [Documentation Home](/) | [Next: Coredeux Export Overview](/modules/coredeux-export/01-overview)
<!-- docs-nav-end -->
