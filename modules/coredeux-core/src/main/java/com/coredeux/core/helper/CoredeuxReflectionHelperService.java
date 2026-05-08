package com.coredeux.core.helper;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Reflection helper contract for framework-level entity inspection.
 */
public interface CoredeuxReflectionHelperService {

    Class<?> getClass(String className);

    Object getFieldValue(String field, Object instance);

    Object getFieldValue(String field, Object instance, Class<?> type);

    void setFieldValue(Field field, Object instance, Object value);

    Field getDeclaredField(String field, Class<?> type);

    List<Field> getAllFields(Class<?> type);
}
