package com.coredeux.impex.handler.impl;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.handler.CoredeuxImportValueHandler;
import com.coredeux.impex.handler.ImportValueContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("jsonMapImportHandler")
public class JsonMapImportHandler implements CoredeuxImportValueHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Converts a JSON object string or an existing map into a map with string keys
     * and supported scalar values.
     */
    @Override
    public Object handle(ImportValueContext context) {
        Object raw = context.getRawValue();
        String columnName = context.getColumn() == null ? null : context.getColumn().getName();
        if (raw == null) {
            return null;
        }
        Class<?> valueType = context.getMapValueType() == null ? Object.class : context.getMapValueType();
        if (raw instanceof Map<?, ?> existing) {
            return convertMap(existing, valueType, columnName);
        }
        if (raw instanceof String s) {
            if (s.isBlank()) {
                return null;
            }
            return convertMap(parseJsonObject(s, columnName), valueType, columnName);
        }
        throw new CoredeuxImportException("Unsupported map import value type: " + raw.getClass().getName()
                + " for column: " + columnName, columnName);
    }

    /**
     * Parses a string value and rejects anything other than a JSON object.
     */
    private Map<?, ?> parseJsonObject(String value, String columnName) {
        try {
            JsonNode node = mapper.readTree(value);
            if (node == null || !node.isObject()) {
                throw new CoredeuxImportException("JSON map value must be an object for column: " + columnName,
                        columnName);
            }
            return mapper.convertValue(node, Map.class);
        } catch (CoredeuxImportException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw new CoredeuxImportException("Failed to parse JSON map for column: " + columnName, columnName,
                    exception);
        }
    }

    /**
     * Copies the source map into a deterministic {@link LinkedHashMap}, enforcing
     * string keys and converting every value to the declared map value type.
     */
    private Map<String, Object> convertMap(Map<?, ?> source, Class<?> valueType, String columnName) {
        Map<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new CoredeuxImportException("Map import keys must be strings for column: " + columnName,
                        columnName);
            }
            converted.put(key, convertMapValue(entry.getValue(), valueType, columnName));
        }
        return converted;
    }

    /**
     * Converts one map value and rejects nested objects/collections because this
     * handler intentionally supports only flat scalar maps.
     */
    private Object convertMapValue(Object value, Class<?> valueType, String columnName) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> || value instanceof Collection<?>) {
            throw new CoredeuxImportException("Map import supports only primitive, boxed, and string values for column: "
                    + columnName, columnName);
        }
        Class<?> boxedType = box(valueType == null ? Object.class : valueType);
        if (Object.class.equals(boxedType)) {
            if (value instanceof String || value instanceof Number || value instanceof Boolean
                    || value instanceof Character) {
                return value;
            }
            throw new CoredeuxImportException("Map import supports only primitive, boxed, and string values for column: "
                    + columnName, columnName);
        }
        return convertScalar(value, boxedType, columnName);
    }

    /**
     * Converts a scalar map value to the map's generic value type.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object convertScalar(Object value, Class<?> targetType, String columnName) {
        String text = String.valueOf(value);
        String trimmed = text.trim();
        try {
            if (String.class.equals(targetType)) {
                return text;
            }
            if (Boolean.class.equals(targetType)) {
                return Boolean.valueOf(trimmed);
            }
            if (Integer.class.equals(targetType)) {
                return Integer.valueOf(trimmed);
            }
            if (Long.class.equals(targetType)) {
                return Long.valueOf(trimmed);
            }
            if (Short.class.equals(targetType)) {
                return Short.valueOf(trimmed);
            }
            if (Byte.class.equals(targetType)) {
                return Byte.valueOf(trimmed);
            }
            if (Double.class.equals(targetType)) {
                return Double.valueOf(trimmed);
            }
            if (Float.class.equals(targetType)) {
                return Float.valueOf(trimmed);
            }
            if (BigInteger.class.equals(targetType)) {
                return new BigInteger(trimmed);
            }
            if (BigDecimal.class.equals(targetType)) {
                return new BigDecimal(trimmed);
            }
            if (UUID.class.equals(targetType)) {
                return UUID.fromString(trimmed);
            }
            if (LocalDate.class.equals(targetType)) {
                return LocalDate.parse(trimmed);
            }
            if (LocalDateTime.class.equals(targetType)) {
                return LocalDateTime.parse(trimmed);
            }
            if (Instant.class.equals(targetType)) {
                return Instant.parse(trimmed);
            }
            if (OffsetDateTime.class.equals(targetType)) {
                return OffsetDateTime.parse(trimmed);
            }
            if (Enum.class.isAssignableFrom(targetType)) {
                return Enum.valueOf((Class<Enum>) targetType, trimmed);
            }
        } catch (RuntimeException exception) {
            throw new CoredeuxImportException("Unable to convert map value to " + targetType.getName()
                    + " for column: " + columnName, columnName, exception);
        }
        throw new CoredeuxImportException("No JSON map conversion available for value type: " + targetType.getName()
                + " for column: " + columnName, columnName);
    }

    /**
     * Converts primitive map value types to boxed classes so conversion can use
     * normal class comparisons.
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
     * Resolves the declared value type of a {@code Map<String, V>} field for JSON
     * map conversion.
     */
    public static Class<?> mapValueType(java.lang.reflect.Field field) {
        if (field == null || !Map.class.isAssignableFrom(field.getType())) {
            return Object.class;
        }
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType parameterizedType) {
            Type argument = parameterizedType.getActualTypeArguments()[1];
            if (argument instanceof Class<?> argumentClass) {
                return argumentClass;
            }
        }
        return Object.class;
    }
}
