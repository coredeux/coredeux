package com.coredeux.impex.service.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.handler.CoredeuxImportValueHandler;
import com.coredeux.impex.handler.ImportValueContext;
import com.coredeux.impex.handler.ImportValueHandlerResolver;
import com.coredeux.impex.handler.impl.DefaultCoredeuxImportValueHandler;
import com.coredeux.impex.handler.impl.JsonMapImportHandler;
import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.model.ImportLookup;
import com.coredeux.impex.model.ImportLog;
import com.coredeux.impex.model.ImportMacro;
import com.coredeux.impex.model.ImportOptions;
import com.coredeux.impex.model.ImportQueryParam;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.model.ImportRow;
import com.coredeux.impex.model.ImportSeverity;
import com.coredeux.impex.model.ImportStatement;
import com.coredeux.impex.service.CoredeuxImportService;

@Service
public class DefaultCoredeuxImportService implements CoredeuxImportService {

    private static final String EQUALS = "EQUALS";
    private static final String ISNULL = "ISNULL";

    private final CoredeuxService coredeuxService;
    private final CoredeuxReflectionHelperService reflectionHelperService;
    private final ImportEntityTargetService entityTargetService;
    private final ImportValueHandlerResolver valueHandlerResolver;

    /**
     * Wires the import orchestrator with core data access, reflection metadata,
     * target writing, and value conversion services.
     */
    public DefaultCoredeuxImportService(CoredeuxService coredeuxService,
            CoredeuxReflectionHelperService reflectionHelperService,
            ImportEntityTargetService entityTargetService,
            ImportValueHandlerResolver valueHandlerResolver) {
        this.coredeuxService = coredeuxService;
        this.reflectionHelperService = reflectionHelperService;
        this.entityTargetService = entityTargetService;
        this.valueHandlerResolver = valueHandlerResolver;
    }

    /**
     * Runs request and statement validation without executing any row mutations.
     */
    @Override
    public ImportResponse validateData(ImportRequest request) {
        ImportResponse response = createResponse();
        validate(request, response);
        return response;
    }

    /**
     * Validates and executes the request across one or more passes, collecting
     * row-level errors into the response.
     */
    @Override
    public ImportResponse importData(ImportRequest request) {
        ImportResponse response = createResponse();
        if (!validate(request, response)) {
            return response;
        }

        ImportOptions options = request.getOptions() == null ? ImportOptions.builder().build() : request.getOptions();
        if (options.isValidateOnly()) {
            return response;
        }
        int passes = Math.max(1, options.getPasses());
        Set<ImportRow> importedRows = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<String, String> references = new LinkedHashMap<>();

        for (int pass = 1; pass <= passes; pass++) {
            for (int statementIndex = 0; statementIndex < request.getStatements().size(); statementIndex++) {
                ImportStatement statement = request.getStatements().get(statementIndex);
                ImportEntityMetadata metadata = entityTargetService.resolveTarget(statement);

                for (int rowIndex = 0; rowIndex < statement.getRows().size(); rowIndex++) {
                    ImportRow row = statement.getRows().get(rowIndex);
                    if (importedRows.contains(row)) {
                        continue;
                    }
                    try {
                        processRow(request, statement, row, metadata, references);
                        importedRows.add(row);
                    } catch (RuntimeException exception) {
                        if (pass == passes) {
                            response.getLogs().add(error(statementIndex, rowIndex, statement, errorColumn(exception),
                                    exception.getMessage(), exception));
                            if (options.isFailFast()) {
                                return response;
                            }
                        }
                    }
                }
            }
        }

        return response;
    }

    /**
     * Performs request-level validation and delegates statement-specific checks to
     * the target service.
     */
    private boolean validate(ImportRequest request, ImportResponse response) {
        if (request == null) {
            response.getLogs().add(error(null, null, null, null, "Import request must not be null", null));
            return false;
        }
        if (request.getStatements() == null || request.getStatements().isEmpty()) {
            response.getLogs().add(error(null, null, null, null, "Import request must contain at least one statement",
                    null));
            return false;
        }
        boolean valid = true;
        for (int i = 0; i < request.getStatements().size(); i++) {
            ImportStatement statement = request.getStatements().get(i);
            try {
                validateStatement(statement);
            } catch (RuntimeException exception) {
                response.getLogs().add(error(i, null, statement, errorColumn(exception), exception.getMessage(),
                        exception));
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Validates one statement and normalizes row storage so execution can assume a
     * non-null row list.
     */
    private void validateStatement(ImportStatement statement) {
        if (statement == null) {
            throw new CoredeuxImportException("Import statement must not be null");
        }
        if (statement.getOperation() == null) {
            throw new CoredeuxImportException("Import statement operation must not be null");
        }
        if (statement.getColumns() == null || statement.getColumns().isEmpty()) {
            throw new CoredeuxImportException("Import statement must contain at least one column");
        }
        if (statement.getRows() == null) {
            statement.setRows(new ArrayList<>());
        }
        entityTargetService.resolveTarget(statement);
    }

    /**
     * Dispatches one row to the operation-specific implementation.
     */
    private void processRow(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportEntityMetadata metadata, Map<String, String> references) {
        switch (statement.getOperation()) {
        case CREATE -> create(request, statement, row, metadata, references);
        case UPSERT -> upsert(request, statement, row, metadata, references);
        case MODIFY -> modify(request, statement, row, metadata, references);
        case DELETE -> delete(request, statement, row, metadata, references);
        case FETCH -> fetch(request, statement, row, metadata, references);
        default -> throw new CoredeuxImportException("Unsupported import operation: " + statement.getOperation());
        }
    }

    /**
     * Creates a fresh target entity, populates all configured columns, saves it,
     * and stores any row-key reference.
     */
    private void create(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportEntityMetadata metadata, Map<String, String> references) {
        Object instance = entityTargetService.createInstance(metadata);
        populateInstance(request, statement, row, metadata, instance, references);
        String id = coredeuxService.save(instance);
        storeReference(row, id, instance, metadata, references);
    }

    /**
     * Finds an existing entity or creates one when no match exists, then writes
     * row values to the resulting entity.
     */
    private void upsert(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportEntityMetadata metadata, Map<String, String> references) {
        Object existing = findExisting(request, statement, row, metadata, references, false);
        if (existing == null) {
            create(request, statement, row, metadata, references);
            return;
        }
        populateInstance(request, statement, row, metadata, existing, references);
        coredeuxService.update(existing);
        storeReference(row, null, existing, metadata, references);
    }

    /**
     * Requires an existing entity, applies row values, and updates it.
     */
    private void modify(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportEntityMetadata metadata, Map<String, String> references) {
        Object existing = findExisting(request, statement, row, metadata, references, true);
        populateInstance(request, statement, row, metadata, existing, references);
        coredeuxService.update(existing);
        storeReference(row, null, existing, metadata, references);
    }

    /**
     * Removes an entity when the row's resolution strategy finds one.
     */
    private void delete(ImportRequest request, ImportStatement statement, ImportRow row, ImportEntityMetadata metadata,
            Map<String, String> references) {
        Object existing = findExisting(request, statement, row, metadata, references, false);
        if (existing != null) {
            coredeuxService.remove(existing);
        }
    }

    /**
     * Finds an existing entity and stores its identifier under the row key for
     * later reference resolution.
     */
    private void fetch(ImportRequest request, ImportStatement statement, ImportRow row, ImportEntityMetadata metadata,
            Map<String, String> references) {
        Object existing = findExisting(request, statement, row, metadata, references, true);
        storeReference(row, null, existing, metadata, references);
    }

    /**
     * Executes the statement's configured resolution strategy and enforces
     * zero/one/many result rules for the operation.
     */
    private Object findExisting(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportEntityMetadata metadata, Map<String, String> references, boolean required) {
        String strategy = resolutionStrategy(statement);
        SearchResult<?> result;
        if ("query".equals(strategy)) {
            result = coredeuxService.query(statement.getQuery().getText(),
                    queryParams(request, statement, row, metadata, references), metadata.getTargetClass(), -1, -1);
        } else {
            List<SearchParams> params = "lookup".equals(strategy)
                    ? lookupParams(request, statement, row, metadata, references)
                    : uniqueParams(request, statement, row, metadata, references);
            result = coredeuxService.loadAll(params, metadata.getTargetClass(), -1, -1);
        }
        List<?> results = result == null || result.getResults() == null ? List.of() : result.getResults();
        if (results.isEmpty()) {
            if (required) {
                throw new CoredeuxImportException("Unable to find entity for " + strategy + " of type: "
                        + metadata.getTargetClass().getName());
            }
            return null;
        }
        if (results.size() > 1) {
            throw new CoredeuxImportException("More than one entity found for " + strategy + " of type: "
                    + metadata.getTargetClass().getName());
        }
        return results.get(0);
    }

    /**
     * Returns the active existing-entity resolution strategy for logging and
     * execution routing.
     */
    private String resolutionStrategy(ImportStatement statement) {
        if (statement.getQuery() != null) {
            return "query";
        }
        if (statement.getLookup() != null && !statement.getLookup().isEmpty()) {
            return "lookup";
        }
        return "unique columns";
    }

    /**
     * Builds custom query parameters by converting the configured source columns
     * from the current row.
     */
    private Map<String, Object> queryParams(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportEntityMetadata metadata, Map<String, String> references) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (Map.Entry<String, ImportQueryParam> entry : statement.getQuery().getParams().entrySet()) {
            String paramName = entry.getKey();
            if (paramName == null || paramName.isBlank()) {
                throw new CoredeuxImportException("Import query parameter name must not be blank");
            }
            ImportColumn column = queryColumn(paramName, entry.getValue(), statement);
            params.put(paramName.trim(), convertedValue(request, statement, row, column, metadata, references));
        }
        return params;
    }

    /**
     * Resolves which import column supplies a query parameter value.
     */
    private ImportColumn queryColumn(String paramName, ImportQueryParam param, ImportStatement statement) {
        String source = param == null ? null : param.getColumn();
        String columnName = source == null || source.isBlank() ? paramName.trim() : source.trim();
        return statement.getColumns().stream()
                .filter(column -> columnName.equals(column.getName()))
                .findFirst()
                .orElseThrow(() -> new CoredeuxImportException("Query parameter '" + paramName
                        + "' source column '" + columnName + "' does not exist in statement columns"));
    }

    /**
     * Builds structured lookup predicates from lookup definitions and current row
     * values.
     */
    private List<SearchParams> lookupParams(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportEntityMetadata metadata, Map<String, String> references) {
        List<SearchParams> params = new ArrayList<>();
        for (ImportLookup lookup : statement.getLookup()) {
            validateLookup(lookup, statement, metadata);
            ImportColumn column = lookupColumn(lookup, statement);
            Object value = convertedValue(request, statement, row, column, metadata, references);
            params.add(SearchParams.builder()
                    .field(lookup.getField().trim())
                    .comparator(value == null && lookup.isNullSearch() ? ISNULL : comparator(lookup))
                    .value(value)
                    .build());
        }
        return params;
    }

    /**
     * Builds equality predicates from all columns marked {@code unique}.
     */
    private List<SearchParams> uniqueParams(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportEntityMetadata metadata, Map<String, String> references) {
        List<SearchParams> params = new ArrayList<>();
        for (ImportColumn column : statement.getColumns()) {
            if (column.isUnique()) {
                Object value = convertedValue(request, statement, row, column, metadata, references);
                params.add(SearchParams.builder()
                        .field(column.getName())
                        .comparator(value == null && column.isNullSearch() ? ISNULL : EQUALS)
                        .value(value)
                        .build());
            }
        }
        return params;
    }

    /**
     * Validates that a lookup predicate targets a real entity field and has a
     * resolvable source import column.
     */
    private void validateLookup(ImportLookup lookup, ImportStatement statement, ImportEntityMetadata metadata) {
        if (lookup == null) {
            throw new CoredeuxImportException("Import lookup must not be null");
        }
        if (lookup.getField() == null || lookup.getField().isBlank()) {
            throw new CoredeuxImportException("Import lookup field must not be blank");
        }
        Field field = reflectionHelperService.getDeclaredField(lookup.getField().trim(), metadata.getTargetClass());
        if (field == null) {
            throw new CoredeuxImportException("Lookup field '" + lookup.getField() + "' does not exist on class: "
                    + metadata.getTargetClass().getName());
        }
        lookupColumn(lookup, statement);
    }

    /**
     * Resolves the source import column for a lookup predicate.
     */
    private ImportColumn lookupColumn(ImportLookup lookup, ImportStatement statement) {
        String source = lookup.getColumn();
        if (source != null && !source.isBlank()) {
            String trimmedSource = source.trim();
            return statement.getColumns().stream()
                    .filter(column -> trimmedSource.equals(column.getName()))
                    .findFirst()
                    .orElseThrow(() -> new CoredeuxImportException("Lookup column '" + source
                            + "' does not exist in statement columns"));
        }
        String field = lookup.getField().trim();
        return statement.getColumns().stream()
                .filter(column -> field.equals(column.getName()))
                .findFirst()
                .orElseThrow(() -> new CoredeuxImportException("Lookup field '" + lookup.getField()
                        + "' does not match any statement column"));
    }

    /**
     * Defaults blank lookup comparators to equality.
     */
    private String comparator(ImportLookup lookup) {
        return lookup.getComparator() == null || lookup.getComparator().isBlank() ? EQUALS
                : lookup.getComparator().trim();
    }

    /**
     * Converts and writes every statement column to a target instance.
     */
    private void populateInstance(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportEntityMetadata metadata, Object instance, Map<String, String> references) {
        for (ImportColumn column : statement.getColumns()) {
            Object value = convertedValue(request, statement, row, column, metadata, references);
            try {
                entityTargetService.writeValue(instance, column, value, metadata);
            } catch (RuntimeException exception) {
                throw columnException(column, exception);
            }
        }
    }

    /**
     * Creates a value context, invokes the configured handler, and verifies the
     * handler output can be assigned to the target field.
     */
    private Object convertedValue(ImportRequest request, ImportStatement statement, ImportRow row, ImportColumn column,
            ImportEntityMetadata metadata, Map<String, String> references) {
        try {
            ImportValueContext context = createValueContext(request, statement, row, column, metadata, references);
            CoredeuxImportValueHandler handler = valueHandlerResolver.resolve(column.getHandler());
            Object value = handler.handle(context);
            validateHandlerOutput(column, metadata, value);
            return value;
        } catch (RuntimeException exception) {
            throw columnException(column, exception);
        }
    }

    /**
     * Builds the context object passed to import value handlers.
     */
    private ImportValueContext createValueContext(ImportRequest request, ImportStatement statement, ImportRow row,
            ImportColumn column, ImportEntityMetadata metadata, Map<String, String> references) {
        Object rawValue = row.getValues() == null ? null : row.getValues().get(column.getName());
        String effectiveValue = effectiveValue(rawValue, column, request);
        Field field = reflectionHelperService.getDeclaredField(column.getName(), metadata.getTargetClass());
        Class<?> expectedType = field == null ? Object.class : field.getType();
        Class<?> elementType = field == null ? String.class : DefaultCoredeuxImportValueHandler.collectionElementType(field);
        Class<?> mapValueType = field == null ? Object.class : JsonMapImportHandler.mapValueType(field);
        return ImportValueContext.builder()
                .rawValue(rawValue)
                .effectiveValue(effectiveValue)
                .expectedType(expectedType)
                .collectionElementType(elementType)
                .mapValueType(mapValueType)
                .targetEntityType(metadata.getTargetClass())
                .column(column)
                .row(row)
                .statement(statement)
                .macros(request == null || request.getMacros() == null ? Map.of() : request.getMacros())
                .references(references == null ? Map.of() : references)
                .build();
    }

    /**
     * Applies default values and macro expansion to the raw row value before type
     * conversion.
     */
    private String effectiveValue(Object rawValue, ImportColumn column, ImportRequest request) {
        String value = rawValue == null ? null : String.valueOf(rawValue);
        if ((value == null || value.isBlank()) && column.getDefaultValue() != null) {
            value = column.getDefaultValue();
        }
        if (value != null && value.startsWith("&") && request != null && request.getMacros() != null
                && request.getMacros().containsKey(value)) {
            ImportMacro macro = request.getMacros().get(value);
            value = macro == null ? null : macro.getValue();
        }
        return value;
    }

    /**
     * Guards against custom handlers returning values that cannot be written to
     * the target field.
     */
    private void validateHandlerOutput(ImportColumn column, ImportEntityMetadata metadata, Object value) {
        Field field = reflectionHelperService.getDeclaredField(column.getName(), metadata.getTargetClass());
        if (field == null) {
            return;
        }
        if (value == null) {
            if (field.getType().isPrimitive()) {
                throw new CoredeuxImportException("Column '" + column.getName()
                        + "' produced null for primitive field: " + field.getName());
            }
            return;
        }
        if (!box(field.getType()).isAssignableFrom(value.getClass())) {
            throw new CoredeuxImportException("Column '" + column.getName() + "' produced " + value.getClass().getName()
                    + ", but target field " + metadata.getTargetClass().getName() + "." + field.getName()
                    + " expects " + field.getType().getName());
        }
    }

    /**
     * Stores a row-key reference after create, upsert, modify, or fetch so later
     * rows can use {@code reference=*}.
     */
    private void storeReference(ImportRow row, String id, Object instance,
            ImportEntityMetadata metadata, Map<String, String> references) {
        if (row.getKey() == null || row.getKey().isBlank()) {
            return;
        }
        String resolvedId = id;
        if (resolvedId == null || resolvedId.isBlank()) {
            Object identifier = entityTargetService.identifier(instance, metadata);
            resolvedId = identifier == null ? null : String.valueOf(identifier);
        }
        if (resolvedId != null) {
            references.put(row.getKey(), resolvedId);
        }
    }

    /**
     * Converts primitive target field types to boxed types for assignability
     * checks.
     */
    private Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (boolean.class.equals(type)) {
            return Boolean.class;
        }
        if (int.class.equals(type)) {
            return Integer.class;
        }
        if (long.class.equals(type)) {
            return Long.class;
        }
        if (short.class.equals(type)) {
            return Short.class;
        }
        if (byte.class.equals(type)) {
            return Byte.class;
        }
        if (double.class.equals(type)) {
            return Double.class;
        }
        if (float.class.equals(type)) {
            return Float.class;
        }
        if (char.class.equals(type)) {
            return Character.class;
        }
        return type;
    }

    /**
     * Creates an empty response with initialized collections from the model
     * builder defaults.
     */
    private ImportResponse createResponse() {
        return ImportResponse.builder().build();
    }

    /**
     * Builds a structured error log entry using one-based statement and row
     * indexes for user-facing diagnostics.
     */
    private ImportLog error(Integer statementIndex, Integer rowIndex, ImportStatement statement, ImportColumn column,
            String message, Throwable throwable) {
        return ImportLog.builder()
                .severity(ImportSeverity.ERROR)
                .statementIndex(statementIndex == null ? null : statementIndex + 1)
                .rowIndex(rowIndex == null ? null : rowIndex + 1)
                .entity(statement == null ? null : statement.getEntity())
                .column(column == null ? null : column.getName())
                .message(message)
                .exceptionType(throwable == null ? null : throwable.getClass().getName())
                .build();
    }

    /**
     * Extracts column context from import exceptions so row errors can point at
     * the failing column.
     */
    private ImportColumn errorColumn(RuntimeException exception) {
        if (exception instanceof CoredeuxImportException importException && importException.getColumn() != null) {
            return ImportColumn.builder().name(importException.getColumn()).build();
        }
        return null;
    }

    /**
     * Wraps lower-level failures with the active column name unless the exception
     * already carries column context.
     */
    private CoredeuxImportException columnException(ImportColumn column, RuntimeException exception) {
        String columnName = column == null ? null : column.getName();
        if (exception instanceof CoredeuxImportException importException
                && importException.getColumn() != null) {
            return importException;
        }
        return new CoredeuxImportException(exception.getMessage(), columnName, exception);
    }
}
