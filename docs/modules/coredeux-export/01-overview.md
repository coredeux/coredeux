# Coredeux Export Overview

<!-- docs-nav-start -->
[Previous: Text And Excel File Import](/modules/coredeux-import/03-text-and-excel-file-import) | [Documentation Home](/) | [Next: Export Request And Output](/modules/coredeux-export/02-request-and-output)
<!-- docs-nav-end -->

`coredeux-export` turns a Coredeux entity into a background export job.
The module is deliberately job-based so applications can queue work, poll for
status, and fetch the stored artifact when it is ready.

The core flow is simple:

1. Build an `ExportRequest`.
2. Call `queueExport(request)`.
3. Read the `uid` from the queued response.
4. Poll `getExport(uid)` until the job is `COMPLETED`.
5. Download or inspect the stored artifact through the active storage backend.

The export service always works with one of two output formats:

- `TEXT`
- `XLSX`

The output is then handed to a storage service. In the current codebase:

- the native demo uses filesystem storage
- the Spring Boot demo uses a database-backed storage service

That difference is intentional. The export module does not care which backend
you choose, as long as the application provides a bean with the expected name.

## What Export Returns

The user-facing response is an `ExportResponse`. It carries the job identity,
state, storage details, row count, timestamps, logs, and any error message.

Useful response fields:

- `uid`
- `status`
- `format`
- `fileName`
- `contentType`
- `storage`
- `rowCount`
- `logs`
- `errorMessage`

That makes export useful both for HTTP APIs and for background workers that
want to show progress in a UI.

## Real Application Shapes

The Spring Boot demo exposes export through a controller:

```java
@PostMapping("/api/export")
public ResponseEntity<ExportResponse> queue(@RequestBody ExportRequest request) {
    return response(exportService.queueExport(request));
}
```

It also exposes a download endpoint backed by the demo storage service:

```java
@GetMapping("/api/export/{uid}/download")
public ResponseEntity<byte[]> download(@PathVariable String uid) {
    ExportStorageRecord record = storageService.findByUid(uid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(record.getContentType()))
            .contentLength(record.getSize())
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + record.getFileName() + "\"")
            .body(record.getContent());
}
```

The native demo keeps the same export idea, but it wires the services directly
in `CoredeuxNativeRuntime` and stores files on disk instead of in a database.

## Default Bean Names

The current export module ships with these default bean names:

- `defaultCoredeuxExportQueueService`
- `defaultCoredeuxExportLogService`
- `consoleCoredeuxExportLogService`
- `defaultCoredeuxExportStorageService`

Those names matter because the resolvers look them up by name when no explicit
value is supplied in the request or configuration.

## Where To Go Next

- [Export Request And Output](/modules/coredeux-export/02-request-and-output)
- [Export Runtime And Configuration](/modules/coredeux-export/03-runtime-and-configuration)
- [Export Reference](/modules/coredeux-export/04-reference)

<!-- docs-nav-start -->
[Previous: Text And Excel File Import](/modules/coredeux-import/03-text-and-excel-file-import) | [Documentation Home](/) | [Next: Export Request And Output](/modules/coredeux-export/02-request-and-output)
<!-- docs-nav-end -->
