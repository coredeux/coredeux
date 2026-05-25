package com.coredeux.core.service.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.service.CoredeuxDataAccessService;

/**
 * Shared base for Coredeux data-access implementations.
 */
public abstract class AbstractCoredeuxDataAccessService implements CoredeuxDataAccessService {

    protected static final String EQUALS = "EQUALS";
    protected static final String NOTEQUALS = "NOTEQUALS";
    protected static final String STARTSWITH = "STARTSWITH";
    protected static final String ANYWHERECS = "ANYWHERECS";
    protected static final String ANYWHERE = "ANYWHERE";
    protected static final String LESSTHANOREQUAL = "LESSTHANOREQUAL";
    protected static final String LESSTHAN = "LESSTHAN";
    protected static final String GREATERTHANOREQUAL = "GREATERTHANOREQUAL";
    protected static final String GREATERTHAN = "GREATERTHAN";
    protected static final String ISNULL = "ISNULL";
    protected static final String ISNOTNULL = "ISNOTNULL";
    protected static final String ISEMPTY = "ISEMPTY";
    protected static final String ISNOTEMPTY = "ISNOTEMPTY";
    protected static final String CONTAINS = "CONTAINS";
    protected static final String NOTCONTAINS = "NOTCONTAINS";

    protected static final Set<String> SUPPORTED_COMPARATORS = Set.of(
            EQUALS,
            NOTEQUALS,
            STARTSWITH,
            ANYWHERECS,
            ANYWHERE,
            LESSTHANOREQUAL,
            LESSTHAN,
            GREATERTHANOREQUAL,
            GREATERTHAN,
            ISNULL,
            ISNOTNULL,
            ISEMPTY,
            ISNOTEMPTY,
            CONTAINS,
            NOTCONTAINS);

    protected final ConcurrentMap<Class<?>, Field> identifierFieldCache = new ConcurrentHashMap<>();

    @Override
    public Set<String> supportedComparators(Class<?> type) {
        return SUPPORTED_COMPARATORS;
    }

    public void validateLoadInput(String id, Class<?> type) {
        if (id == null || id.isBlank()) {
            throw new CoredeuxValidationException("Load identifier must not be blank");
        }
        validateSearchType(type);
    }

    public void validateQueryInput(String query, Class<?> type) {
        if (query == null || query.isBlank()) {
            throw new CoredeuxValidationException("Query string must not be blank");
        }
        validateSearchType(type);
    }

    public void validateSearchType(Class<?> type) {
        if (type == null) {
            throw new CoredeuxValidationException("Entity type must not be null");
        }
    }

    public <T> void validateEntity(T entity, String action) {
        if (entity == null) {
            throw new CoredeuxValidationException("Entity must not be null for " + action);
        }
    }

    public String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new CoredeuxValidationException(message);
        }
        return value.trim();
    }

    public <T> List<T> defaultResults(List<T> results) {
        return results == null || results.isEmpty() ? List.of() : results;
    }

    public PaginationData buildPagination(long totalResults, long resultSize, int pageSize, int currentPage) {
        PaginationData paginationData = new PaginationData();
        paginationData.setCurrentPage((long) currentPage);
        paginationData.setPageSize((long) pageSize);
        if (isPagingEnabled(pageSize, currentPage)) {
            paginationData.setTotalResults(totalResults);
            paginationData.setResultSize(resultSize);
            paginationData.setTotalPages(resultSize == 0 ? 0L : (long) Math.ceil(resultSize / (double) pageSize));
        }
        return paginationData;
    }

    public boolean isPagingEnabled(int pageSize, int currentPage) {
        return pageSize > 0 && currentPage > 0;
    }

    public String typeName(Class<?> type) {
        return type == null ? "null" : type.getName();
    }

    public CoredeuxDataAccessException wrap(String message, Throwable exception) {
        if (exception instanceof CoredeuxDataAccessException dataAccessException) {
            return dataAccessException;
        }
        if (exception instanceof CoredeuxValidationException validationException) {
            throw validationException;
        }
        return new CoredeuxDataAccessException(message, exception);
    }

    public Object extractIdentifier(Object entity) {
        Field field = identifierField(entity.getClass());
        makeAccessible(field);
        return readField(field, entity);
    }

    public Object requireIdentifier(Object entity, String action) {
        Object identifier = extractIdentifier(entity);
        if (identifier == null || (identifier instanceof String stringIdentifier && stringIdentifier.isBlank())) {
            throw new CoredeuxValidationException("Entity identifier must not be blank for " + action);
        }
        return identifier;
    }

    public void setIdentifier(Object entity, Object identifier) {
        Field field = identifierField(entity.getClass());
        makeAccessible(field);
        writeField(field, entity, convertValue(identifier, field.getType()));
    }

    public Field identifierField(Class<?> type) {
        return identifierFieldCache.computeIfAbsent(type, this::findIdentifierField);
    }

    public Field findIdentifierField(Class<?> type) {
        Field annotated = findAnnotatedField(type,
                "org.springframework.data.annotation.Id",
                "jakarta.persistence.Id",
                "javax.persistence.Id");
        if (annotated != null) {
            return annotated;
        }
        Field fallback = findField(type, "id");
        if (fallback == null) {
            throw new CoredeuxValidationException("Unable to resolve identifier field for class: " + type.getName());
        }
        return fallback;
    }

    public Field findAnnotatedField(Class<?> type, String... annotationClassNames) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                for (String annotationClassName : annotationClassNames) {
                    if (hasAnnotation(field, annotationClassName)) {
                        return field;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public boolean hasAnnotation(Field field, String annotationClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends java.lang.annotation.Annotation> annotationType =
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName(annotationClassName);
            return field.isAnnotationPresent(annotationType);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public void doWithFields(Class<?> type, Consumer<Field> consumer) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                consumer.accept(field);
            }
            current = current.getSuperclass();
        }
    }

    public void copyFields(Object source, Object target) {
        Class<?> current = source.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                Field targetField = findField(target.getClass(), field.getName());
                if (targetField == null || java.lang.reflect.Modifier.isStatic(targetField.getModifiers())) {
                    continue;
                }
                makeAccessible(field);
                makeAccessible(targetField);
                writeField(targetField, target, convertValue(readField(field, source), targetField.getType()));
            }
            current = current.getSuperclass();
        }
    }

    public void makeAccessible(Field field) {
        if (field != null) {
            field.setAccessible(true);
        }
    }

    public Object readField(Field field, Object target) {
        try {
            makeAccessible(field);
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new CoredeuxValidationException("Unable to read field: " + field.getName(), exception);
        }
    }

    public void writeField(Field field, Object target, Object value) {
        try {
            makeAccessible(field);
            field.set(target, value);
        } catch (IllegalAccessException exception) {
            throw new CoredeuxValidationException("Unable to write field: " + field.getName(), exception);
        }
    }

    public Object convertIdentifier(String value, Class<?> targetType) {
        if (targetType == null || targetType == String.class || targetType.isAssignableFrom(String.class)) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(value);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(value);
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.valueOf(value);
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.valueOf(value);
        }
        if (targetType == BigInteger.class) {
            return new BigInteger(value);
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(value);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value);
        }
        if (targetType == UUID.class) {
            return UUID.fromString(value);
        }
        if (targetType.isEnum()) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) targetType, value);
            return enumValue;
        }
        Object factoryValue = invokeStringFactory(targetType, value);
        if (factoryValue != null) {
            return factoryValue;
        }
        throw new CoredeuxValidationException("Unsupported identifier type: " + targetType.getName());
    }

    public Object convertValue(Object value, Class<?> targetType) {
        if (value == null || targetType == null) {
            return value;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(String.valueOf(value));
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(String.valueOf(value));
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.valueOf(String.valueOf(value));
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.valueOf(String.valueOf(value));
        }
        if (targetType == BigInteger.class) {
            return new BigInteger(String.valueOf(value));
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(String.valueOf(value));
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.valueOf(String.valueOf(value));
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.valueOf(String.valueOf(value));
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(String.valueOf(value));
        }
        if (targetType == UUID.class) {
            return UUID.fromString(String.valueOf(value));
        }
        if (targetType.isEnum()) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) targetType, String.valueOf(value));
            return enumValue;
        }
        Object factoryValue = invokeStringFactory(targetType, String.valueOf(value));
        return factoryValue != null ? factoryValue : value;
    }

    public Object invokeStringFactory(Class<?> targetType, String value) {
        for (String methodName : List.of("valueOf", "of", "fromString")) {
            try {
                java.lang.reflect.Method factoryMethod = targetType.getMethod(methodName, String.class);
                factoryMethod.setAccessible(true);
                return factoryMethod.invoke(null, value);
            } catch (ReflectiveOperationException exception) {
                try {
                    java.lang.reflect.Method factoryMethod = targetType.getDeclaredMethod(methodName, String.class);
                    factoryMethod.setAccessible(true);
                    return factoryMethod.invoke(null, value);
                } catch (ReflectiveOperationException ignored) {
                    // try next factory
                }
            }
        }
        return null;
    }

    protected String normalizeComparatorsKey(String comparator) {
        return comparator == null ? null : comparator.trim().toUpperCase(Locale.ROOT);
    }
}
