package com.coredeux.impex.service.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.model.ImportLookup;
import com.coredeux.impex.model.ImportOperation;
import com.coredeux.impex.model.ImportQueryParam;
import com.coredeux.impex.model.ImportRow;
import com.coredeux.impex.model.ImportStatement;

public class ImportEntityTargetService {

    private final CoredeuxReflectionHelperService reflectionHelperService;
    private final EntityDefinitionRegistry entityDefinitionRegistry;

    /**
     * Wires reflection and entity-definition services used to validate and mutate
     * target entities.
     */
    public ImportEntityTargetService(CoredeuxReflectionHelperService reflectionHelperService,
            EntityDefinitionRegistry entityDefinitionRegistry) {
        this.reflectionHelperService = reflectionHelperService;
        this.entityDefinitionRegistry = entityDefinitionRegistry;
    }

    /**
     * Resolves the statement's target class and validates all target-side import
     * assumptions before execution.
     */
    public ImportEntityMetadata resolveTarget(ImportStatement statement) {
        if (statement == null || statement.getEntity() == null || statement.getEntity().isBlank()) {
            throw new CoredeuxImportException("Import statement entity must not be blank");
        }
        Class<?> targetClass = reflectionHelperService.getClass(statement.getEntity().trim());
        String identifier = entityDefinitionRegistry.findByEntityType(targetClass)
                .map(CoredeuxEntityDefinition::getIdentifier)
                .orElse("id");
        validateRows(statement);
        validateColumns(statement, targetClass);
        validateLookup(statement, targetClass);
        validateQuery(statement);
        validateResolutionStrategy(statement);
        return ImportEntityMetadata.builder()
                .targetClass(targetClass)
                .identifierPath(identifier)
                .statement(statement)
                .build();
    }

    /**
     * Verifies row keys are either absent or meaningful non-blank aliases.
     */
    private void validateRows(ImportStatement statement) {
        if (statement.getRows() == null) {
            return;
        }
        for (ImportRow row : statement.getRows()) {
            if (row != null && row.getKey() != null && row.getKey().isBlank()) {
                throw new CoredeuxImportException("Import row key must not be blank");
            }
        }
    }

    /**
     * Validates that every import column is unique, named, and maps to an actual
     * field on the target entity.
     */
    private void validateColumns(ImportStatement statement, Class<?> targetClass) {
        Set<String> names = new LinkedHashSet<>();
        for (ImportColumn column : statement.getColumns()) {
            if (column == null) {
                throw new CoredeuxImportException("Import column must not be null");
            }
            if (column.getName() == null || column.getName().isBlank()) {
                throw new CoredeuxImportException("Import column name must not be blank");
            }
            String columnName = column.getName().trim();
            if (!names.add(columnName)) {
                throw new CoredeuxImportException("Duplicate import column name: " + columnName, columnName);
            }
            Field field = reflectionHelperService.getDeclaredField(columnName, targetClass);
            if (field == null) {
                throw new CoredeuxImportException("Column '" + columnName + "' maps to missing field '"
                        + columnName + "' on class: " + targetClass.getName(), columnName);
            }
            validateColumnMode(column, field);
        }
    }

    /**
     * Ensures collection modes are valid and only applied to collection fields
     * when the mode needs collection behavior.
     */
    private void validateColumnMode(ImportColumn column, Field field) {
        String mode = column.getMode() == null || column.getMode().isBlank() ? "replace"
                : column.getMode().trim().toLowerCase();
        if (!Set.of("replace", "append", "clear").contains(mode)) {
            throw new CoredeuxImportException("Unsupported collection mode '" + column.getMode()
                    + "' for column: " + column.getName(), column.getName());
        }
        if (!"replace".equals(mode) && !Collection.class.isAssignableFrom(field.getType())) {
            throw new CoredeuxImportException("Collection mode '" + mode + "' can only be used for collection column: "
                    + column.getName(), column.getName());
        }
    }

    /**
     * Enforces the rule that non-create operations use exactly one lookup
     * strategy: unique columns, structured lookup, or custom query.
     */
    private void validateResolutionStrategy(ImportStatement statement) {
        if (ImportOperation.CREATE.equals(statement.getOperation())) {
            return;
        }
        int strategies = 0;
        if (statement.getColumns().stream().anyMatch(ImportColumn::isUnique)) {
            strategies++;
        }
        if (statement.getLookup() != null && !statement.getLookup().isEmpty()) {
            strategies++;
        }
        if (hasQuery(statement)) {
            strategies++;
        }
        if (strategies == 0) {
            throw new CoredeuxImportException(
                    "At least one existing-entity resolution strategy is required for operation: "
                            + statement.getOperation() + ". Configure unique columns, lookup, or query.");
        }
        if (strategies > 1) {
            throw new CoredeuxImportException(
                    "Only one existing-entity resolution strategy is allowed per statement: unique, lookup, or query");
        }
    }

    /**
     * Validates structured lookup declarations against target fields and source
     * statement columns.
     */
    private void validateLookup(ImportStatement statement, Class<?> targetClass) {
        if (statement.getLookup() == null) {
            return;
        }
        for (ImportLookup lookup : statement.getLookup()) {
            if (lookup == null) {
                throw new CoredeuxImportException("Import lookup must not be null");
            }
            if (lookup.getField() == null || lookup.getField().isBlank()) {
                throw new CoredeuxImportException("Import lookup field must not be blank");
            }
            String field = lookup.getField().trim();
            if (reflectionHelperService.getDeclaredField(field, targetClass) == null) {
                throw new CoredeuxImportException("Lookup field '" + lookup.getField() + "' does not exist on class: "
                        + targetClass.getName());
            }
            resolveLookupColumn(lookup, statement);
        }
    }

    /**
     * Resolves the import column that supplies a lookup value, falling back to the
     * lookup field name when no source column is declared.
     */
    private ImportColumn resolveLookupColumn(ImportLookup lookup, ImportStatement statement) {
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
     * Validates custom query text and verifies every query parameter can be
     * derived from a statement column.
     */
    private void validateQuery(ImportStatement statement) {
        if (!hasQuery(statement)) {
            return;
        }
        if (statement.getQuery().getText() == null || statement.getQuery().getText().isBlank()) {
            throw new CoredeuxImportException("Import query text must not be blank");
        }
        if (statement.getQuery().getParams() == null || statement.getQuery().getParams().isEmpty()) {
            throw new CoredeuxImportException("Import query must define at least one parameter");
        }
        for (Map.Entry<String, ImportQueryParam> entry : statement.getQuery().getParams().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new CoredeuxImportException("Import query parameter name must not be blank");
            }
            resolveQueryColumn(entry.getKey(), entry.getValue(), statement);
        }
    }

    /**
     * Resolves the source import column for a query parameter, using the parameter
     * name when no explicit source column is configured.
     */
    private ImportColumn resolveQueryColumn(String paramName, ImportQueryParam param, ImportStatement statement) {
        String source = param == null ? null : param.getColumn();
        String columnName = source == null || source.isBlank() ? paramName.trim() : source.trim();
        return statement.getColumns().stream()
                .filter(column -> columnName.equals(column.getName()))
                .findFirst()
                .orElseThrow(() -> new CoredeuxImportException("Query parameter '" + paramName
                        + "' source column '" + columnName + "' does not exist in statement columns"));
    }

    /**
     * Determines whether a statement uses the custom query resolution strategy.
     */
    private boolean hasQuery(ImportStatement statement) {
        return statement.getQuery() != null;
    }

    /**
     * Creates a new target entity instance for CREATE or missing UPSERT rows.
     */
    public Object createInstance(ImportEntityMetadata metadata) {
        try {
            Constructor<?> constructor = metadata.getTargetClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new CoredeuxImportException("Unable to create instance of " + metadata.getTargetClass().getName(),
                    exception);
        }
    }

    /**
     * Writes one converted column value to the target entity, delegating collection
     * behavior to the collection-specific writer.
     */
    public void writeValue(Object target, ImportColumn column, Object value, ImportEntityMetadata metadata) {
        Field field = reflectionHelperService.getDeclaredField(column.getName(), metadata.getTargetClass());
        if (field == null) {
            throw new CoredeuxImportException("Unable to resolve field: " + column.getName());
        }
        if (Collection.class.isAssignableFrom(field.getType())) {
            writeCollectionValue(target, column, value, metadata, field);
            return;
        }
        if (!isAssignable(field.getType(), value)) {
            String actualType = value == null ? "null" : value.getClass().getName();
            throw new CoredeuxImportException("Column '" + column.getName() + "' produced " + actualType
                    + ", but target field " + metadata.getTargetClass().getName() + "." + field.getName()
                    + " expects " + field.getType().getName());
        }
        reflectionHelperService.setFieldValue(field, target, value);
    }

    /**
     * Applies replace, append, or clear semantics for collection fields.
     */
    @SuppressWarnings("unchecked")
    private void writeCollectionValue(Object target, ImportColumn column, Object value, ImportEntityMetadata metadata,
            Field field) {
        String mode = column.getMode() == null || column.getMode().isBlank() ? "replace"
                : column.getMode().trim().toLowerCase();
        if ("clear".equals(mode)) {
            clearCollectionValue(target, field);
            return;
        }
        if (value != null && !(value instanceof Collection<?>)) {
            throw new CoredeuxImportException("Column '" + column.getName() + "' produced " + value.getClass().getName()
                    + ", but target field " + metadata.getTargetClass().getName() + "." + field.getName()
                    + " expects " + field.getType().getName());
        }

        Collection<Object> incoming = value == null ? null : (Collection<Object>) value;
        switch (mode) {
        case "replace" -> {
            if (!isAssignable(field.getType(), incoming)) {
                String actualType = incoming == null ? "null" : incoming.getClass().getName();
                throw new CoredeuxImportException("Column '" + column.getName() + "' produced " + actualType
                        + ", but target field " + metadata.getTargetClass().getName() + "." + field.getName()
                        + " expects " + field.getType().getName());
            }
            reflectionHelperService.setFieldValue(field, target, incoming);
        }
        case "append" -> appendCollectionValue(target, incoming, field);
        default -> throw new CoredeuxImportException("Unsupported collection mode '" + column.getMode()
                + "' for column: " + column.getName());
        }
    }

    /**
     * Clears an existing collection or initializes an empty collection when the
     * target field is currently null.
     */
    @SuppressWarnings("unchecked")
    private void clearCollectionValue(Object target, Field field) {
        Collection<Object> existing = (Collection<Object>) reflectionHelperService.getFieldValue(field.getName(),
                target, target.getClass());
        if (existing == null) {
            reflectionHelperService.setFieldValue(field, target, createCollection(field.getType()));
            return;
        }
        existing.clear();
    }

    /**
     * Appends incoming collection items to the existing target collection,
     * creating a compatible collection when needed.
     */
    @SuppressWarnings("unchecked")
    private void appendCollectionValue(Object target, Collection<Object> incoming, Field field) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        Collection<Object> existing = (Collection<Object>) reflectionHelperService.getFieldValue(field.getName(),
                target, target.getClass());
        if (existing == null) {
            existing = createCollection(field.getType());
            reflectionHelperService.setFieldValue(field, target, existing);
        }
        existing.addAll(incoming);
    }

    /**
     * Creates the concrete collection type used when the target collection field
     * is null.
     */
    private Collection<Object> createCollection(Class<?> collectionType) {
        if (Set.class.isAssignableFrom(collectionType)) {
            return new LinkedHashSet<>();
        }
        return new ArrayList<>();
    }

    /**
     * Reads the identifier value from an imported or fetched entity for row-key
     * reference storage.
     */
    public Object identifier(Object target, ImportEntityMetadata metadata) {
        return reflectionHelperService.getFieldValue(metadata.getIdentifierPath(), target, metadata.getTargetClass());
    }

    /**
     * Checks whether a converted value can be assigned to a target field,
     * accounting for primitive fields.
     */
    private boolean isAssignable(Class<?> targetType, Object value) {
        if (value == null) {
            return !targetType.isPrimitive();
        }
        return box(targetType).isAssignableFrom(value.getClass());
    }

    /**
     * Converts primitive field types to boxed classes for assignability checks.
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
}
