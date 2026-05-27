# Export Runtime And Configuration

<!-- docs-nav-start -->
[Previous: Export Request And Output](/02-request-and-output) | [Documentation Home](/) | [Next: Export Reference](/coredeux-export-reference)
<!-- docs-nav-end -->

This page explains how export is wired in Spring Boot and in the native demo.
The module has the same export engine in both cases, but the host application
decides how the queue, log, and storage services are created.

## Spring Boot Wiring

The Spring Boot starter builds the export stack in
`CoredeuxExportAutoConfiguration`.

The starter uses:

- `CoredeuxExportProperties`
- `Environment`
- `CoredeuxProperties`

That means Spring properties can override `META-INF/coredeux.yml`, and the
core YAML still acts as a native fallback source.

Important configuration keys:

- `coredeux.export.default-format`
- `coredeux.export.log.default-service`
- `coredeux.export.log.base-directory`
- `coredeux.export.queue.base-directory`
- `coredeux.export.storage.default-service`
- `coredeux.export.storage.filesystem.base-directory`
- `coredeux.export.worker.enabled`
- `coredeux.export.worker.max-parallel`
- `coredeux.export.worker.delay-ms`

### Spring Boot Demo Example

`examples/coredeux-spring-boot-demo/src/main/resources/application.yml` uses:

```yaml
coredeux:
  export:
    default-format: TEXT
    log:
      default-service: defaultCoredeuxExportLogService
    storage:
      default-service: databaseCoredeuxExportStorageService
      filesystem:
        base-directory: ${COREDEUX_EXPORT_FILESYSTEM_BASE_DIRECTORY:${java.io.tmpdir}/coredeux-export}
    worker:
      enabled: true
      max-parallel: 2
      delay-ms: 3000
```

The demo controller then downloads the stored artifact from the database
storage service:

```java
@GetMapping("/api/export/{uid}/download")
public ResponseEntity<byte[]> download(@PathVariable String uid) { ... }
```

### Starter Bean Names

The starter creates or resolves the following beans:

- `defaultCoredeuxExportQueueService`
- `defaultCoredeuxExportLogService`
- `consoleCoredeuxExportLogService`
- `defaultCoredeuxExportStorageService`
- `defaultCoredeuxExportValueHandler`

It also wires:

- `TextExportWriter`
- `ExcelExportWriter`
- `DefaultCoredeuxExportWorker`
- `CoredeuxExportWorkerScheduler`

## Native Wiring

The native runtime creates the same export pieces directly in
`CoredeuxNativeRuntime`.

### Native Demo Example

`examples/coredeux-java-native-demo/src/main/resources/META-INF/coredeux.yml`
contains:

```yaml
coredeux:
  export:
    default-format: TEXT
    log:
      default-service: defaultCoredeuxExportLogService
    storage:
      default-service: defaultCoredeuxExportStorageService
      filesystem:
        base-directory: ${java.io.tmpdir}/coredeux-export
    worker:
      enabled: true
      delay-ms: 5000
      max-parallel: 2
```

In native mode, the runtime registers the export services in the component
registry, for example:

```java
InMemoryCoredeuxComponentRegistry.builder()
        .component(DefaultCoredeuxExportStorageServiceResolver.DEFAULT_STORAGE_SERVICE,
                new DefaultCoredeuxFileSystemExportStorageService(exportBaseDirectory(coredeuxProperties)))
        .component(DefaultCoredeuxExportLogServiceResolver.DEFAULT_LOG_SERVICE,
                new FileCoredeuxExportLogService(exportLogDirectory(coredeuxProperties)))
        .component(FileCoredeuxExportQueueService.DEFAULT_QUEUE_SERVICE,
                new FileCoredeuxExportQueueService(exportQueueDirectory(coredeuxProperties)))
        .build();
```

That is the native rule in practice:

- the YAML chooses the bean name
- the host application decides how to construct the bean

## Queue, Log, Storage

Export is split into three infrastructure pieces:

### Queue

The queue stores export jobs and tracks status transitions:

- `NEW`
- `IN_PROGRESS`
- `COMPLETED`
- `ERROR`

The file-backed queue service writes to a JSON file in the configured queue
directory.

### Log

The log service stores per-job log lines and exposes `findByUid(uid)` so the
response can include them.

The export response therefore doubles as a status object and as a log summary.

### Storage

The storage service receives the generated file and returns an
`ExportStorageArtifact`.

The artifact is what a caller can use to locate the export file, read the
content type, and inspect backend metadata.

## Worker

The worker claims queued jobs and executes them in the background.

The Spring scheduler is controlled by:

```java
@Scheduled(fixedDelayString = "#{@coredeuxExportProperties.workerDelayMs}")
```

The native runtime starts its own daemon thread when worker execution is
enabled.

The worker configuration defaults are:

- enabled: `true`
- max parallel: `2`
- delay: `5000 ms`

When needed, tests or custom hosts can call `processPendingExports()`
manually.

## Custom Backends

You can override the defaults with your own beans.

### Database Storage Example

The Spring Boot demo uses a database-backed storage service:

```java
@Service("databaseCoredeuxExportStorageService")
public class DatabaseCoredeuxExportStorageService implements CoredeuxExportStorageService {
    ...
}
```

The service stores the file bytes, the content type, the generated file name,
and the metadata JSON in a JPA entity.

### Custom Log Example

You can register a custom log service the same way:

```java
@Service("databaseCoredeuxExportLogService")
public class DatabaseCoredeuxExportLogService implements CoredeuxExportLogService {
    ...
}
```

Then point `coredeux.export.log.default-service` at that bean name.

### Per-Request Storage Selection

When a request needs a specific storage backend, set:

```json
{
  "options": {
    "storageService": "customStorageService"
  }
}
```

If no request override is given, Coredeux falls back to the configured default
storage service.

<!-- docs-nav-start -->
[Previous: Export Request And Output](/02-request-and-output) | [Documentation Home](/) | [Next: Export Reference](/coredeux-export-reference)
<!-- docs-nav-end -->
