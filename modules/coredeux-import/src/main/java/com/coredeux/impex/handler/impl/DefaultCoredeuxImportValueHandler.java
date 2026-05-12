package com.coredeux.impex.handler.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.handler.CoredeuxImportValueHandler;
import com.coredeux.impex.handler.ImportValueContext;

public class DefaultCoredeuxImportValueHandler implements CoredeuxImportValueHandler {

    private static final String EQUALS = "EQUALS";

    private final CoredeuxService coredeuxService;

    /**
     * Receives the Coredeux service used to resolve entity references during value
     * conversion.
     */
    public DefaultCoredeuxImportValueHandler(CoredeuxService coredeuxService) {
        this.coredeuxService = coredeuxService;
    }

    /**
     * Converts the current column value, delegating to reference resolution when
     * the column declares {@code reference}.
     */
    @Override
    public Object handle(ImportValueContext context) {
        if (context.getColumn().getReference() != null && !context.getColumn().getReference().isBlank()) {
            return convertReference(context);
        }
        return convertValue(context.getEffectiveValue(), context.getExpectedType(), context);
    }

    /**
     * Resolves reference columns either as a single entity or as a collection of
     * referenced entities.
     */
    private Object convertReference(ImportValueContext context) {
        Class<?> expectedType = context.getExpectedType();
        if (Collection.class.isAssignableFrom(expectedType)) {
            Collection<Object> values = Set.class.isAssignableFrom(expectedType) ? new LinkedHashSet<>()
                    : new ArrayList<>();
            for (String part : splitCsv(context.getEffectiveValue())) {
                if (!part.isBlank()) {
                    values.add(resolveSingleReference(part.trim(), context.getCollectionElementType(), context));
                }
            }
            return values;
        }
        if (context.getEffectiveValue() == null || context.getEffectiveValue().isBlank()) {
            return null;
        }
        return resolveSingleReference(context.getEffectiveValue().trim(), expectedType, context);
    }

    /**
     * Resolves one reference token using row-key references, compound references,
     * or a single-field equality lookup.
     */
    private Object resolveSingleReference(String value, Class<?> referenceType, ImportValueContext context) {
        if ("*".equals(context.getColumn().getReference())) {
            String id = context.getReferences().get(value);
            if (id == null || id.isBlank()) {
                throw new CoredeuxImportException("Unable to resolve import reference: " + value);
            }
            return coredeuxService.load(id, referenceType);
        }

        String reference = context.getColumn().getReference().trim();
        if (reference.contains(":")) {
            return resolveCompoundReference(value, referenceType, context, reference);
        }

        Object parsedValue = convertValue(value, fieldType(referenceType, reference), context);
        SearchResult<?> result = coredeuxService
                .loadAll(List.of(SearchParams.builder().field(reference).comparator(EQUALS).value(parsedValue).build()),
                        referenceType, -1, -1);
        return requireSingleResult(result, referenceType, reference + "=" + value);
    }

    /**
     * Resolves references declared as multiple colon-separated target fields.
     *
     * <p>
     * Escaped colons are preserved for literal values and split only at true
     * compound-reference separators.
     */
    private Object resolveCompoundReference(String value, Class<?> referenceType, ImportValueContext context,
            String reference) {
        List<String> fields = splitEscaped(reference, ':');
        List<String> values = splitEscaped(value, ':');
        if (values.size() < fields.size()) {
            throw new CoredeuxImportException("Reference value '" + value + "' does not match reference '" + reference
                    + "'");
        }
        List<SearchParams> params = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i).trim();
            Object parsedValue = convertValue(values.get(i).trim(), fieldType(referenceType, field), context);
            params.add(SearchParams.builder().field(field).comparator(EQUALS).value(parsedValue).build());
        }
        SearchResult<?> result = coredeuxService.loadAll(params, referenceType, -1, -1);
        return requireSingleResult(result, referenceType, reference + "=" + value);
    }

    /**
     * Enforces that a reference lookup produces exactly one matching entity.
     */
    private Object requireSingleResult(SearchResult<?> result, Class<?> type, String description) {
        List<?> results = result == null || result.getResults() == null ? List.of() : result.getResults();
        if (results.isEmpty()) {
            throw new CoredeuxImportException("No entity found for " + description + " of type: " + type.getName());
        }
        if (results.size() > 1) {
            throw new CoredeuxImportException("More than one entity found for " + description + " of type: "
                    + type.getName());
        }
        return results.get(0);
    }

    /**
     * Converts a string value to the target Java type supported by the default
     * handler.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object convertValue(String value, Class<?> targetType, ImportValueContext context) {
        if (targetType == null || Object.class.equals(targetType)) {
            return value;
        }
        if (value == null || value.isBlank()) {
            return primitiveDefault(targetType);
        }
        String trimmed = value.trim();
        Class<?> boxedType = box(targetType);
        if (String.class.equals(boxedType)) {
            return value;
        }
        if (Boolean.class.equals(boxedType)) {
            return Boolean.valueOf(trimmed);
        }
        if (Integer.class.equals(boxedType)) {
            return Integer.valueOf(trimmed);
        }
        if (Long.class.equals(boxedType)) {
            return Long.valueOf(trimmed);
        }
        if (Short.class.equals(boxedType)) {
            return Short.valueOf(trimmed);
        }
        if (Byte.class.equals(boxedType)) {
            return Byte.valueOf(trimmed);
        }
        if (Double.class.equals(boxedType)) {
            return Double.valueOf(trimmed);
        }
        if (Float.class.equals(boxedType)) {
            return Float.valueOf(trimmed);
        }
        if (BigInteger.class.equals(boxedType)) {
            return new BigInteger(trimmed);
        }
        if (BigDecimal.class.equals(boxedType)) {
            return new BigDecimal(trimmed);
        }
        if (UUID.class.equals(boxedType)) {
            return UUID.fromString(trimmed);
        }
        if (LocalDate.class.equals(boxedType)) {
            return LocalDate.parse(trimmed);
        }
        if (LocalDateTime.class.equals(boxedType)) {
            return LocalDateTime.parse(trimmed);
        }
        if (Instant.class.equals(boxedType)) {
            return Instant.parse(trimmed);
        }
        if (OffsetDateTime.class.equals(boxedType)) {
            return OffsetDateTime.parse(trimmed);
        }
        if (Enum.class.isAssignableFrom(boxedType)) {
            return Enum.valueOf((Class<Enum>) boxedType, trimmed);
        }
        if (Collection.class.isAssignableFrom(boxedType)) {
            Collection<Object> values = Set.class.isAssignableFrom(boxedType) ? new LinkedHashSet<>() : new ArrayList<>();
            Class<?> elementType = context.getCollectionElementType() == null ? String.class
                    : context.getCollectionElementType();
            for (String part : splitCsv(value)) {
                if (!part.isBlank()) {
                    values.add(convertValue(part.trim(), elementType, context));
                }
            }
            return values;
        }
        throw new CoredeuxImportException("No default import conversion available from String to " + targetType.getName()
                + " for column: " + context.getColumn().getName());
    }

    /**
     * Finds a field type in the referenced entity hierarchy so reference values
     * are converted before lookup.
     */
    private Class<?> fieldType(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName).getType();
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }
        return String.class;
    }

    /**
     * Splits comma-separated collection input while honoring escaped commas.
     */
    private List<String> splitCsv(String value) {
        return splitEscaped(value, ',');
    }

    /**
     * Splits text on a separator while preserving escaped separators and trailing
     * backslashes.
     */
    private List<String> splitEscaped(String value, char separator) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (escaping) {
                if (character != separator && character != '\\') {
                    current.append('\\');
                }
                current.append(character);
                escaping = false;
            } else if (character == '\\') {
                escaping = true;
            } else if (character == separator) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (escaping) {
            current.append('\\');
        }
        parts.add(current.toString());
        return parts;
    }

    /**
     * Supplies Java's default value for primitive fields when the import value is
     * blank.
     */
    private Object primitiveDefault(Class<?> targetType) {
        if (!targetType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(targetType)) {
            return false;
        }
        if (char.class.equals(targetType)) {
            return '\0';
        }
        return 0;
    }

    /**
     * Converts primitive types to their boxed equivalents for assignability and
     * conversion checks.
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
     * Resolves the declared element type of a collection field for collection item
     * conversion.
     */
    public static Class<?> collectionElementType(java.lang.reflect.Field field) {
        if (field == null) {
            return String.class;
        }
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType parameterizedType) {
            Type argument = parameterizedType.getActualTypeArguments()[0];
            if (argument instanceof Class<?> argumentClass) {
                return argumentClass;
            }
        }
        return String.class;
    }

}
