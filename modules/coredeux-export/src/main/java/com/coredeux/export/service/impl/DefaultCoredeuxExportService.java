package com.coredeux.export.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.log.CoredeuxExportLogServiceResolver;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportJob;
import com.coredeux.export.model.ExportOptions;
import com.coredeux.export.model.ExportQuery;
import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;
import com.coredeux.export.model.ExportStatus;
import com.coredeux.export.model.ExportStorageArtifact;
import com.coredeux.export.model.ExportStorageRequest;
import com.coredeux.export.queue.CoredeuxExportQueueService;
import com.coredeux.export.service.CoredeuxExportExecutionService;
import com.coredeux.export.service.CoredeuxExportService;
import com.coredeux.export.storage.CoredeuxExportStorageService;
import com.coredeux.export.storage.CoredeuxExportStorageServiceResolver;
import com.coredeux.export.writer.ExcelExportWriter;
import com.coredeux.export.writer.ExportWriteSession;
import com.coredeux.export.writer.ExportWriter;
import com.coredeux.export.writer.TextExportWriter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultCoredeuxExportService implements CoredeuxExportService, CoredeuxExportExecutionService {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final CoredeuxService coredeuxService;
    private final CoredeuxReflectionHelperService reflectionHelperService;
    private final EntityDefinitionRegistry entityDefinitionRegistry;
    private final ExportFieldPathParser fieldPathParser;
    private final ExportValueResolver valueResolver;
    private final CoredeuxExportStorageServiceResolver storageServiceResolver;
    private final CoredeuxExportLogServiceResolver logServiceResolver;
    private final CoredeuxExportQueueService queueService;
    private final TextExportWriter textWriter;
    private final ExcelExportWriter excelWriter;
    private final ExportFormat defaultFormat;

    public DefaultCoredeuxExportService(CoredeuxService coredeuxService,
            CoredeuxReflectionHelperService reflectionHelperService, EntityDefinitionRegistry entityDefinitionRegistry,
            ExportFieldPathParser fieldPathParser, ExportValueResolver valueResolver,
            CoredeuxExportStorageServiceResolver storageServiceResolver,
            CoredeuxExportLogServiceResolver logServiceResolver, CoredeuxExportQueueService queueService,
            TextExportWriter textWriter, ExcelExportWriter excelWriter) {
        this(coredeuxService, reflectionHelperService, entityDefinitionRegistry, fieldPathParser, valueResolver,
                storageServiceResolver, logServiceResolver, queueService, textWriter, excelWriter,
                ExportFormat.TEXT);
    }

    public DefaultCoredeuxExportService(CoredeuxService coredeuxService,
            CoredeuxReflectionHelperService reflectionHelperService, EntityDefinitionRegistry entityDefinitionRegistry,
            ExportFieldPathParser fieldPathParser, ExportValueResolver valueResolver,
            CoredeuxExportStorageServiceResolver storageServiceResolver,
            CoredeuxExportLogServiceResolver logServiceResolver, CoredeuxExportQueueService queueService,
            TextExportWriter textWriter, ExcelExportWriter excelWriter, ExportFormat defaultFormat) {
        this.coredeuxService = coredeuxService;
        this.reflectionHelperService = reflectionHelperService;
        this.entityDefinitionRegistry = entityDefinitionRegistry;
        this.fieldPathParser = fieldPathParser;
        this.valueResolver = valueResolver;
        this.storageServiceResolver = storageServiceResolver;
        this.logServiceResolver = logServiceResolver;
        this.queueService = queueService;
        this.textWriter = textWriter;
        this.excelWriter = excelWriter;
        this.defaultFormat = defaultFormat == null ? ExportFormat.TEXT : defaultFormat;
    }

    @Override
    public ExportResponse queueExport(ExportRequest request) {
        validateRequest(request);
        Class<?> entityType = resolveEntityType(request.getEntity());
        List<ExportFieldPath> fieldPaths = fieldPathParser.parse(request.getFieldList());
        validateFieldPaths(fieldPaths, entityType);
        ExportJob job = queueService.enqueue(request);
        CoredeuxExportLogService logService = logService();
        logService.info(job.getUid(), "Export queued", Map.of("entity", request.getEntity()));
        return responseFromJob(queueService.findByUid(job.getUid()).orElse(job));
    }

    @Override
    public ExportResponse getExport(String uid) {
        ExportJob job = queueService.findByUid(uid)
                .orElseThrow(() -> new CoredeuxExportException("Unknown export uid: " + uid));
        return responseFromJob(job);
    }

    @Override
    public ExportResponse execute(String uid, ExportRequest request) {
        validateRequest(request);
        ExportOptions options = normalizeOptions(request.getOptions());
        Class<?> entityType = resolveEntityType(request.getEntity());
        List<ExportFieldPath> fieldPaths = fieldPathParser.parse(request.getFieldList());
        validateFieldPaths(fieldPaths, entityType);
        ExportWriter writer = writer(options.getFormat());
        Path tempFile = createTempFile(writer);
        long rowCount = 0;
        Instant startedAt = Instant.now();
        CoredeuxExportLogService logService = logService();
        queueService.updateProgress(uid, 0);
        logService.info(uid, "Export execution started", Map.of("entity", request.getEntity(),
                "format", options.getFormat().name(), "storageService", storageServiceName(options)));
        try (ExportWriteSession session = writer.open(tempFile, options)) {
            List<String> headers = fieldPaths.stream().map(ExportFieldPath::expression).toList();
            session.writeHeader(headers);
            int page = 1;
            int batchSize = batchSize(options);
            int limit = limit(options);
            while (limit < 0 || rowCount < limit) {
                int pageSize = limit < 0 ? batchSize : Math.min(batchSize, (int) (limit - rowCount));
                SearchResult<?> result = loadPage(request, entityType, pageSize, page++);
                if (result == null || CollectionUtils.isEmpty(result.getResults())) {
                    break;
                }
                for (Object entity : result.getResults()) {
                    session.writeRow(createRow(entity, entityType, fieldPaths, options, request));
                    rowCount++;
                    if (limit >= 0 && rowCount >= limit) {
                        break;
                    }
                }
                queueService.updateProgress(uid, rowCount);
                logService.info(uid, "Export page processed", Map.of("rowCount", rowCount));
                if (result.getResults().size() < pageSize) {
                    break;
                }
            }
            session.finish();
        } catch (IOException exception) {
            log.error("Export execution failed while writing file for entity {}", request.getEntity(), exception);
            markExportError(uid, request, tempFile, logService, "Unable to write export file", exception);
            throw new CoredeuxExportException("Unable to write export file", exception);
        } catch (Exception exception) {
            log.error("Export execution failed while processing entity {}", request.getEntity(), exception);
            markExportError(uid, request, tempFile, logService, "Unable to execute export", exception);
            throw exception instanceof CoredeuxExportException coredeuxExportException
                    ? coredeuxExportException
                    : new CoredeuxExportException("Unable to execute export", exception);
        }

        try {
            CoredeuxExportStorageService storageService = storageServiceResolver.resolve(options.getStorageService());
            ExportStorageArtifact artifact = storageService.store(ExportStorageRequest.builder()
                    .uid(uid)
                    .sourceFile(tempFile)
                    .fileName(fileName(options, writer))
                    .contentType(writer.contentType())
                    .format(options.getFormat())
                    .build());
            ExportResponse response = ExportResponse.builder()
                    .uid(uid)
                    .status(ExportStatus.COMPLETED)
                    .format(options.getFormat())
                    .fileName(artifact.getFileName())
                    .contentType(artifact.getContentType())
                    .storage(artifact)
                    .rowCount(rowCount)
                    .fields(fieldPaths.stream().map(ExportFieldPath::expression).toList())
                    .createdAt(startedAt)
                    .startedAt(startedAt)
                    .completedAt(Instant.now())
                    .build();
            queueService.markCompleted(uid, response);
            logService.info(uid, "Export completed", Map.of("rowCount", rowCount, "storageType",
                    artifact.getStorageType() == null ? "" : artifact.getStorageType()));
            deleteQuietly(tempFile);
            return responseFromJob(queueService.findByUid(uid).orElseThrow());
        } catch (RuntimeException exception) {
            queueService.markError(uid, exception.getMessage());
            logService.error(uid, "Unable to store export file", exception, Map.of("entity", request.getEntity()));
            deleteQuietly(tempFile);
            throw exception;
        }
    }

    private void markExportError(String uid, ExportRequest request, Path tempFile, CoredeuxExportLogService logService,
            String message, Exception exception) {
        queueService.markError(uid, exception.getMessage());
        logService.error(uid, message, exception, Map.of("entity", request.getEntity()));
        deleteQuietly(tempFile);
    }

    private void validateRequest(ExportRequest request) {
        if (request == null) {
            throw new CoredeuxExportException("Export request must not be null");
        }
        if (request.getEntity() == null || request.getEntity().isBlank()) {
            throw new CoredeuxExportException("Export entity must not be blank");
        }
        if (request.getFieldList() == null || request.getFieldList().isEmpty()) {
            throw new CoredeuxExportException("Export fieldList must not be empty");
        }
        boolean hasSearchParams = request.getSearchParams() != null && !request.getSearchParams().isEmpty();
        boolean hasQuery = request.getQuery() != null && request.getQuery().getText() != null
                && !request.getQuery().getText().isBlank();
        if (hasSearchParams && hasQuery) {
            throw new CoredeuxExportException("Use either searchParams or query, not both");
        }
        if (request.getQuery() != null && !hasQuery) {
            throw new CoredeuxExportException("Export query text must not be blank");
        }
    }

    private ExportOptions normalizeOptions(ExportOptions options) {
        ExportOptions normalized = options == null ? ExportOptions.builder().build() : options;
        if (normalized.getFormat() == null) {
            normalized.setFormat(defaultFormat);
        }
        if (normalized.getBatchSize() == null || normalized.getBatchSize() <= 0) {
            normalized.setBatchSize(DEFAULT_BATCH_SIZE);
        }
        if (normalized.getIncludeHeader() == null) {
            normalized.setIncludeHeader(true);
        }
        if (normalized.getTextSeparator() == null || normalized.getTextSeparator().isEmpty()) {
            normalized.setTextSeparator("|");
        }
        if (normalized.getCollectionSeparator() == null) {
            normalized.setCollectionSeparator(", ");
        }
        return normalized;
    }

    private Class<?> resolveEntityType(String entity) {
        Class<?> entityType = reflectionHelperService.getClass(entity.trim());
        entityDefinitionRegistry.findByEntityType(entityType)
                .orElseThrow(() -> new CoredeuxExportException("Entity is not registered with Coredeux: "
                        + entityType.getName()));
        return entityType;
    }

    private void validateFieldPaths(List<ExportFieldPath> fieldPaths, Class<?> entityType) {
        for (ExportFieldPath path : fieldPaths) {
            validateFieldPath(path, entityType, 0, false);
        }
    }

    private void validateFieldPath(ExportFieldPath path, Class<?> type, int index, boolean collectionAlreadySeen) {
        if (index >= path.segments().size()) {
            return;
        }
        String segment = path.segments().get(index);
        java.lang.reflect.Field field = reflectionHelperService.getDeclaredField(segment, type);
        if (field == null) {
            throw new CoredeuxExportException("Export field '" + segment + "' does not exist on " + type.getName());
        }
        Class<?> fieldType = field.getType();
        if (java.util.Collection.class.isAssignableFrom(fieldType)) {
            if (collectionAlreadySeen) {
                throw new CoredeuxExportException("Nested collection export paths are not supported: "
                        + path.expression());
            }
            Class<?> elementType = genericType(field);
            if (index + 1 < path.segments().size()) {
                validateFieldPath(path, elementType, index + 1, true);
            }
            return;
        }
        if (java.util.Map.class.isAssignableFrom(fieldType)) {
            return;
        }
        if (index + 1 < path.segments().size()) {
            validateFieldPath(path, fieldType, index + 1, collectionAlreadySeen);
        }
    }

    private Class<?> genericType(java.lang.reflect.Field field) {
        java.lang.reflect.Type genericType = field.getGenericType();
        if (genericType instanceof java.lang.reflect.ParameterizedType parameterizedType) {
            java.lang.reflect.Type[] arguments = parameterizedType.getActualTypeArguments();
            if (arguments.length > 0 && arguments[arguments.length - 1] instanceof Class<?> type) {
                return type;
            }
        }
        throw new CoredeuxExportException("Unable to resolve collection element type for field: " + field.getName());
    }

    private SearchResult<?> loadPage(ExportRequest request, Class<?> entityType, int pageSize, int page) {
        ExportQuery query = request.getQuery();
        if (query != null && query.getText() != null && !query.getText().isBlank()) {
            Map<String, Object> params = query.getParams() == null ? Map.of() : query.getParams();
            return coredeuxService.query(query.getText().trim(), params, entityType, pageSize, page);
        }
        List<SearchParams> params = request.getSearchParams() == null ? List.of() : request.getSearchParams();
        return coredeuxService.loadAll(params, entityType, pageSize, page);
    }

    private List<String> createRow(Object entity, Class<?> entityType, List<ExportFieldPath> fieldPaths,
            ExportOptions options, ExportRequest request) {
        List<String> row = new ArrayList<>();
        for (ExportFieldPath path : fieldPaths) {
            row.add(valueResolver.resolve(entity, entityType, path, options.getCollectionSeparator(), request));
        }
        return row;
    }

    private int batchSize(ExportOptions options) {
        return options.getBatchSize() == null || options.getBatchSize() <= 0 ? DEFAULT_BATCH_SIZE
                : options.getBatchSize();
    }

    private int limit(ExportOptions options) {
        return options.getLimit() == null || options.getLimit() < 0 ? -1 : options.getLimit();
    }

    private ExportWriter writer(ExportFormat format) {
        return ExportFormat.XLSX.equals(format) ? excelWriter : textWriter;
    }

    private Path createTempFile(ExportWriter writer) {
        try {
            return Files.createTempFile("coredeux-export-", writer.extension());
        } catch (IOException exception) {
            throw new CoredeuxExportException("Unable to create temporary export file", exception);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private String fileName(ExportOptions options, ExportWriter writer) {
        if (options.getFileName() != null && !options.getFileName().isBlank()) {
            return options.getFileName().trim();
        }
        return "coredeux-export" + writer.extension();
    }

    private ExportResponse responseFromJob(ExportJob job) {
        ExportResponse response = job.getResponse();
        if (response == null) {
            response = ExportResponse.builder()
                    .uid(job.getUid())
                    .status(job.getStatus())
                    .errorMessage(job.getErrorMessage())
                    .createdAt(job.getCreatedAt())
                    .startedAt(job.getStartedAt())
                    .completedAt(job.getCompletedAt())
                    .build();
        }
        response.setUid(job.getUid());
        response.setStatus(job.getStatus());
        response.setErrorMessage(job.getErrorMessage());
        response.setCreatedAt(job.getCreatedAt());
        response.setStartedAt(job.getStartedAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setLogs(logService().findByUid(job.getUid()));
        return response;
    }

    private CoredeuxExportLogService logService() {
        return logServiceResolver.resolve(null);
    }

    private String storageServiceName(ExportOptions options) {
        return options == null || options.getStorageService() == null ? "defaultCoredeuxExportStorageService"
                : options.getStorageService();
    }
}
