# Coredeux Export Guide

<!-- docs-nav-start -->
[Previous: Coredeux Export](README.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Platform Documentation](../../platform/README.md)
<!-- docs-nav-end -->

`coredeux-export` turns Coredeux entities into an export job that is queued,
processed in the background, and then exposed through an `ExportResponse`
looked up by uid. The response contains the job state, exported field names,
row count, file/storage details, logs, and any failure message.

The module supports two output formats:

- `TEXT`: pipe-separated text by default.
- `XLSX`: Excel workbook with one sheet named `export`.

## Basic Request

Export all customers with a few fields:

```json
{
  "entity": "com.example.Customer",
  "fieldList": [
    { "path": "name" },
    { "path": "email" },
    { "path": "active" },
    { "path": "profile:biography" }
  ],
  "options": {
    "format": "TEXT",
    "includeHeader": true
  }
}
```

`entity` is the fully qualified Java class name of a Coredeux-registered entity.
`fieldList` is the list of field paths to return. Each field can optionally
declare a handler and metadata.

The request is queued through `exportService.queueExport(request)`. The returned
response contains a uid and `NEW` status. The background worker later updates
the same job to `IN_PROGRESS`, `COMPLETED`, or `ERROR`.

Use `exportService.getExport(uid)` to read the current status and result.

## Query Strategies

Use zero or one query strategy.

When no query strategy is supplied, export loads all rows for the entity through
`CoredeuxService.loadAll` with an empty filter list.

When filtering is needed, use exactly one of:

- `searchParams`
- `query`

Coredeux rejects a request that supplies both. This keeps export resolution
explicit and avoids mixing structured framework filters with backend-specific
query text.

## Search Params

Use `searchParams` when the query can be expressed with Coredeux structured
comparators:

```json
{
  "entity": "com.example.Customer",
  "fieldList": [
    { "path": "name" },
    { "path": "email" },
    { "path": "status" }
  ],
  "searchParams": [
    {
      "field": "active",
      "comparator": "EQUALS",
      "value": true
    },
    {
      "field": "email",
      "comparator": "ANYWHERE",
      "value": "@example.com"
    }
  ],
  "options": {
    "format": "XLSX",
    "batchSize": 100
  }
}
```

Comparator support depends on the selected `CoredeuxDataAccessService`
implementation. For JPA, the current implementation supports comparators such
as `EQUALS`, `NOTEQUALS`, `STARTSWITH`, `ANYWHERE`, `LESSTHAN`,
`GREATERTHAN`, `ISNULL`, `ISNOTNULL`, `ISEMPTY`, `ISNOTEMPTY`, `CONTAINS`, and
`NOTCONTAINS`.

## Query

Use `query` when the backend should execute a query expression directly:

```json
{
  "entity": "com.example.Customer",
  "fieldList": [
    { "path": "name" },
    { "path": "email" },
    { "path": "profile:legacySignupDate" }
  ],
  "query": {
    "text": "select c from Customer c where c.active = :active",
    "params": {
      "active": true
    }
  }
}
```

`query.params` is optional. A self-contained query is valid:

```json
{
  "entity": "com.example.Customer",
  "fieldList": [
    { "path": "name" },
    { "path": "email" }
  ],
  "query": {
    "text": "select c from Customer c where c.active = true"
  }
}
```

The meaning of `query.text` is owned by the active data-access implementation.
For `coredeux-core-jpa`, it is JPQL typed to the requested entity because the JPA
implementation delegates to `EntityManager.createQuery(query, entityType)`. A
future MongoDB or Elasticsearch implementation can interpret the same field as
that backend's query language.

## Async Execution

Export execution is asynchronous by default. The module uses a queue service to
persist export jobs and a worker to claim `NEW` jobs in parallel up to the
configured throttle.

Default queue storage:

- `defaultCoredeuxExportQueueService`

Default log service:

- `defaultCoredeuxExportLogService`
- `consoleCoredeuxExportLogService`

Default file storage:

- `defaultCoredeuxExportStorageService`

The filesystem storage backend writes into a configurable base directory. Set
`coredeux.export.storage.filesystem.base-directory` to choose where the exported
files are copied. If you do not set it, Coredeux falls back to a temp-based
directory under the JVM temp folder.

Export log output is selected separately from storage. The demo and default
runtime wiring resolve the log service by name, using
`coredeux.export.log.default-service` to choose the active bean. The default is
`defaultCoredeuxExportLogService`, and you can switch to
`consoleCoredeuxExportLogService` when you want logs only in the application
console.

## Custom Log Service

Create a custom export log service when you want to persist logs to a database,
send them to an external system, or redact entries before they are stored.

Implement `CoredeuxExportLogService`:

```java
public interface CoredeuxExportLogService {
    void info(String uid, String message, Map<String, Object> metadata);
    void warn(String uid, String message, Map<String, Object> metadata);
    void error(String uid, String message, Throwable error, Map<String, Object> metadata);
    List<ExportLogEntry> findByUid(String uid);
}
```

Register the bean with a stable Spring name, then point
`coredeux.export.log.default-service` to it:

```java
@Service("databaseCoredeuxExportLogService")
public class DatabaseCoredeuxExportLogService implements CoredeuxExportLogService {
    // persist info/warn/error entries and return them from findByUid(...)
}
```

```yaml
coredeux:
  export:
    log:
      default-service: databaseCoredeuxExportLogService
```

`findByUid(...)` matters because the export response can surface log entries
back to the caller. If your service only writes logs and does not read them
back, return an empty list and document that limitation for your users.

The active storage backend can be selected per request through:

```json
{
  "options": {
    "storageService": "customStorageService"
  }
}
```

If no storage service is specified, Coredeux falls back to the configured
default storage service.

Worker settings:

- `coredeux.export.worker.enabled` defaults to `true`
- `coredeux.export.worker.max-parallel` defaults to `2`
- `coredeux.export.worker.delay-ms` defaults to `5000`

The worker processes queued jobs in pages, so the database is never read all at
once.

If your application relies on the scheduled worker, make sure scheduling is
enabled in the Spring application context. The worker also exposes
`processPendingExports()` for manual invocation in tests or custom schedulers.

## Field Paths

Each `fieldList` entry uses compact path syntax:

```json
[
  { "path": "name" },
  { "path": "email" },
  { "path": "active" },
  { "path": "profile:biography" },
  { "path": "profile:legacySignupDate" }
]
```

Rules:

- `name` reads the root entity field `name`.
- `profile:biography` reads `customer.profile.biography`.
- `preferences:locale` reads key `locale` from map field `preferences`.
- `roles:code` reads `code` from each item in collection field `roles` and
  joins the values into one cell.
- A path can contain at most one collection hop. Nested collections are rejected
  because one cell cannot represent that shape deterministically.
- When a handler is used, Coredeux resolves the complete path first. For
  `profile:legacySignupDate`, the handler receives the value of
  `legacySignupDate`, not the `profile` object.

Use backslash escaping in field expressions when a literal colon is part of a
field/map key name:

```text
metadata:key\\:withColon
```

## Field Handlers

Every exported field passes through an export value handler. If no handler is
specified, Coredeux uses `defaultCoredeuxExportValueHandler`.

The default handler covers the major common output types:

- `null` becomes an empty cell.
- `BigDecimal` uses `toPlainString()`.
- `java.util.Date` uses ISO instant text.
- Java time values use their `toString()` representation.
- enums use `name()`.
- everything else is returned as-is and then formatted with `String.valueOf`.

Use a custom handler when a field needs masking, formatting, localization,
legacy date patterns, redaction, code-to-label mapping, or any other
application-specific output transformation.

Structured field example:

```json
{
  "entity": "com.example.Customer",
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

Handler contract:

```java
public interface CoredeuxExportValueHandler {
    Object handle(ExportValueContext context);
}
```

`ExportValueContext` provides:

- `rootEntity`: the original exported entity.
- `resolvedValue`: the final value resolved by the field path.
- `fieldPath`: the full field path expression.
- `entityType`: root entity class.
- `field`: the `ExportField` definition, including metadata.
- `request`: the full export request.

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
        Instant instant = ((Date) value).toInstant();
        return formatter.withZone(ZoneOffset.UTC).format(instant);
    }
}
```

## Collection Output

Primitive collections and collection references are flattened into a single
cell:

```text
phoneNumbers
```

could produce:

```text
111, 222, 333
```

For object collections:

```text
roles:code
```

could produce:

```text
ADMIN, USER
```

The default collection separator is `, `. Override it with:

```json
{
  "options": {
    "collectionSeparator": " | "
  }
}
```

## Text Output

Text output uses `|` as the default cell separator:

```text
name|email|profile:biography
Jane|jane@example.com|Demo profile
```

Change the separator with:

```json
{
  "options": {
    "format": "TEXT",
    "textSeparator": "|"
  }
}
```

Cells containing the separator, double quotes, or line breaks are quoted. Double
quotes inside quoted cells are doubled.

## Excel Output

Excel output writes the same rows to an `.xlsx` workbook:

```json
{
  "entity": "com.example.Customer",
  "fieldList": [
    { "path": "name" },
    { "path": "email" },
    { "path": "active" }
  ],
  "options": {
    "format": "XLSX",
    "fileName": "customers.xlsx"
  }
}
```

Each exported field becomes one column. When `includeHeader` is true, the first
row contains the field expressions exactly as supplied.

## Options

Supported options:

- `format`: `TEXT` or `XLSX`. Default is `TEXT`.
- `limit`: maximum number of rows to export. Missing or negative means no
  explicit export-side limit.
- `batchSize`: page size used when loading data. Default is `100`.
- `includeHeader`: whether to write the field names as the first row. Default
  is `true`.
- `textSeparator`: separator for `TEXT` output. Default is `|`.
- `collectionSeparator`: separator used when flattening collections. Default is
  `, `.
- `storageService`: optional storage backend bean name. Defaults to
  `defaultCoredeuxExportStorageService`.
- `filesystem.base-directory`: filesystem storage destination used by
  `defaultCoredeuxExportStorageService`. Defaults to a temp directory under the
  JVM temp folder.
- `fileName`: optional output file name. Defaults to `coredeux-export.txt` or
  `coredeux-export.xlsx`.

## Java Usage

```java
ExportRequest request = ExportRequest.builder()
        .entity(Customer.class.getName())
        .fieldList(List.of(
                ExportField.builder().path("name").build(),
                ExportField.builder().path("email").build(),
                ExportField.builder().path("active").build(),
                ExportField.builder().path("profile:biography").build()))
        .searchParams(List.of(new SearchParams("active", "EQUALS", true)))
        .options(ExportOptions.builder()
                .format(ExportFormat.XLSX)
                .batchSize(100)
                .build())
        .build();

ExportResponse queued = exportService.queueExport(request);
ExportResponse response = exportService.getExport(queued.getUid());
```

The caller can return `response.getStorage()` details from an HTTP controller
using `response.getStatus()`, `response.getUid()`, `response.getContentType()`,
and `response.getFileName()`.

`ExportResponse.storage` exposes details such as:

- where the file was written
- the storage type
- absolute and relative paths when available
- a URL and canonical URL when the backend can provide them

That artifact is what cloud storage implementations will adapt in the future.

`ExportStorageArtifact.metadata` and `ExportResponse.metadata` are open-ended
maps for backend-specific details such as provider IDs, object version ids,
checksums, or any other runtime note you want to preserve.

<!-- docs-nav-start -->
[Previous: Coredeux Export](README.md) | [Documentation Home](../../README.md) | [Tutorial Order](../../SUMMARY.md) | [Next: Platform Documentation](../../platform/README.md)
<!-- docs-nav-end -->
