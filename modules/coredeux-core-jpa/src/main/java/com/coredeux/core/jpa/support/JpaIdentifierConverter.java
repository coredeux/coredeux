package com.coredeux.core.jpa.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import com.coredeux.core.exceptions.CoredeuxValidationException;

/**
 * Utility for converting string identifiers into JPA entity identifier types.
 */
public final class JpaIdentifierConverter {

    private JpaIdentifierConverter() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object convert(String identifier, Class<?> targetType) {
        if (identifier == null) {
            return null;
        }
        if (targetType == null) {
            throw new CoredeuxValidationException("Identifier target type must not be null");
        }
        if (String.class.equals(targetType)) {
            return identifier;
        }
        if (Long.class.equals(targetType) || long.class.equals(targetType)) {
            return Long.valueOf(identifier);
        }
        if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
            return Integer.valueOf(identifier);
        }
        if (Short.class.equals(targetType) || short.class.equals(targetType)) {
            return Short.valueOf(identifier);
        }
        if (Byte.class.equals(targetType) || byte.class.equals(targetType)) {
            return Byte.valueOf(identifier);
        }
        if (Double.class.equals(targetType) || double.class.equals(targetType)) {
            return Double.valueOf(identifier);
        }
        if (Float.class.equals(targetType) || float.class.equals(targetType)) {
            return Float.valueOf(identifier);
        }
        if (BigInteger.class.equals(targetType)) {
            return new BigInteger(identifier);
        }
        if (BigDecimal.class.equals(targetType)) {
            return new BigDecimal(identifier);
        }
        if (UUID.class.equals(targetType)) {
            return UUID.fromString(identifier);
        }
        if (Enum.class.isAssignableFrom(targetType)) {
            return Enum.valueOf((Class<Enum>) targetType.asSubclass(Enum.class), identifier);
        }
        try {
            Method valueOf = targetType.getMethod("valueOf", String.class);
            return valueOf.invoke(null, identifier);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Constructor<?> constructor = targetType.getConstructor(String.class);
            return constructor.newInstance(identifier);
        } catch (ReflectiveOperationException exception) {
            throw new CoredeuxValidationException(
                    "Unsupported identifier type conversion for class: " + targetType.getName(), exception);
        }
    }
}
