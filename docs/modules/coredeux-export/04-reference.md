# Coredeux Export Reference

<!-- docs-nav-start -->
[Previous: Export Runtime And Configuration](/03-runtime-and-configuration) | [Documentation Home](/) | [Next: Native Getting Started In 10 Minutes](/01-native-getting-started-in-10-minutes)
<!-- docs-nav-end -->

This is the technical reference for `coredeux-export` and the export starter.
Use it when you need the exact contracts, defaults, bean names, and runtime
flow.

## Public Contracts

### Service Interfaces

- `CoredeuxExportService`
- `CoredeuxExportExecutionService`
- `CoredeuxExportQueueService`
- `CoredeuxExportLogService`
- `CoredeuxExportLogServiceResolver`
- `CoredeuxExportStorageService`
- `CoredeuxExportStorageServiceResolver`
- `CoredeuxExportValueHandler`
- `ExportValueHandlerResolver`

### Model Types

- `ExportRequest`
- `ExportField`
- `ExportQuery`
- `ExportOptions`
- `ExportFormat`
- `ExportStatus`
- `ExportJob`
- `ExportResponse`
- `ExportStorageArtifact`
- `ExportStorageRequest`
- `ExportLogEntry`

## Execution Flow

The current `DefaultCoredeuxExportService` executes in this order:

1. Validate the request.
2. Resolve the entity type.
3. Parse field paths.
4. Queue the export job.
5. Load data through `CoredeuxService`.
6. Resolve field values.
7. Write a temporary file.
8. Store the artifact through the chosen storage service.
9. Persist the final `ExportResponse`.
10. Return the response with logs attached.

That is why export is easy to extend without changing the request contract.

## Default Implementations

### Writers

- `TextExportWriter`
- `ExcelExportWriter`

`TextExportWriter` writes pipe-separated text by default.
`ExcelExportWriter` writes an `export` worksheet in an `.xlsx` workbook.

### Queue / Log / Storage

- `FileCoredeuxExportQueueService`
- `FileCoredeuxExportLogService`
- `DefaultCoredeuxFileSystemExportStorageService`
- `DefaultCoredeuxExportLogServiceResolver`
- `DefaultCoredeuxExportStorageServiceResolver`

### Value Resolution

- `ExportFieldPathParser`
- `ExportValueFormatter`
- `ExportValueResolver`
- `DefaultCoredeuxExportValueHandler`

## Bean Names

The export module uses stable bean names so Spring Boot and native hosts can
agree on the same wiring contract.

| Bean name | Purpose |
| --- | --- |
| `defaultCoredeuxExportQueueService` | Queue implementation used when no override is selected |
| `defaultCoredeuxExportLogService` | File-based log service |
| `consoleCoredeuxExportLogService` | Console-only log service |
| `defaultCoredeuxExportStorageService` | File-system storage service |
| `defaultCoredeuxExportValueHandler` | Default export value handler |

## Configuration Keys

### Spring and Native Shared Keys

- `coredeux.export.default-format`
- `coredeux.export.log.default-service`
- `coredeux.export.log.base-directory`
- `coredeux.export.queue.base-directory`
- `coredeux.export.storage.default-service`
- `coredeux.export.storage.filesystem.base-directory`
- `coredeux.export.worker.enabled`
- `coredeux.export.worker.max-parallel`
- `coredeux.export.worker.delay-ms`

### Request-Specific Keys

- `options.format`
- `options.limit`
- `options.batchSize`
- `options.includeHeader`
- `options.textSeparator`
- `options.collectionSeparator`
- `options.storageService`
- `options.fileName`

## Defaults

The current code defaults to:

- `ExportFormat.TEXT`
- `batchSize = 100`
- `includeHeader = true`
- `textSeparator = "|"`
- `collectionSeparator = ", "`
- `fileName = coredeux-export.txt` or `coredeux-export.xlsx`
- queue base directory under the JVM temp folder
- log base directory under the JVM temp folder
- filesystem storage under the JVM temp folder

## Current Demo Mapping

### Spring Boot Demo

- export storage bean: `databaseCoredeuxExportStorageService`
- export log bean: `defaultCoredeuxExportLogService`
- export output format: `TEXT`
- worker delay: `3000`

### Native Demo

- export storage bean: `defaultCoredeuxExportStorageService`
- export log bean: `defaultCoredeuxExportLogService`
- export output format: `TEXT`
- worker delay: `5000`

The names are different because the apps intentionally use different storage
implementations, but the Coredeux export contract stays the same.

## Extension Points

### Custom Value Handler

Use a custom handler when the exported value needs formatting, redaction, or a
legacy code-to-label mapping.

The same export field contract can also route to a `.drl` handler when the
DRL starter is active. Native handlers still implement
`CoredeuxExportValueHandler`; DRL-backed handlers use the same field name and
are resolved through the shared value-handler service.

### Custom Storage

Implement `CoredeuxExportStorageService` when you want database, cloud, or
object-store support.

### Custom Log Service

Implement `CoredeuxExportLogService` when you want a persistent audit trail or
external log sink.

### Custom Queue

Implement `CoredeuxExportQueueService` when the job store should be something
other than the file-backed queue.

## Short Code Map

- `queueExport(...)` queues work and returns a job snapshot.
- `getExport(uid)` reads the persisted job.
- `execute(uid, request)` performs the export.
- `DefaultCoredeuxExportWorker.processPendingExports()` claims queued jobs.
- `CoredeuxExportWorkerScheduler.poll()` triggers the Spring scheduled poll.
- `DefaultCoredeuxFileSystemExportStorageService.store(...)` persists the generated file.

## Practical Example

The simplest end-to-end usage in Java is:

```java
ExportResponse queued = exportService.queueExport(request);
ExportResponse completed = exportService.getExport(queued.getUid());
```

The caller can then inspect:

- `completed.getStatus()`
- `completed.getFileName()`
- `completed.getContentType()`
- `completed.getStorage()`
- `completed.getLogs()`

Those fields are the reason the module works cleanly in both HTTP controllers
and direct host applications.

<!-- docs-nav-start -->
[Previous: Export Runtime And Configuration](/03-runtime-and-configuration) | [Documentation Home](/) | [Next: Native Getting Started In 10 Minutes](/01-native-getting-started-in-10-minutes)
<!-- docs-nav-end -->
