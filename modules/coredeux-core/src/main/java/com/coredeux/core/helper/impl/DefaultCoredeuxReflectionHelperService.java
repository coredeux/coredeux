package com.coredeux.core.helper.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;

/**
 * Default reflection helper used by the framework for entity/property access.
 */
@Service
public class DefaultCoredeuxReflectionHelperService implements CoredeuxReflectionHelperService {

    @Override
    public Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new CoredeuxStrategyException("Unable to resolve class: " + className, exception);
        }
    }

    @Override
    public Object getFieldValue(String field, Object instance) {
        if (instance == null) {
            return null;
        }
        return getFieldValue(field, instance, instance.getClass());
    }

    @Override
    public Object getFieldValue(String field, Object instance, Class<?> type) {
        if (instance == null || field == null || field.isBlank()) {
            return null;
        }

        try {
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                if (field.equals(descriptor.getName())) {
                    Method readMethod = descriptor.getReadMethod();
                    if (readMethod == null) {
                        break;
                    }
                    readMethod.setAccessible(true);
                    return readMethod.invoke(instance);
                }
            }
        } catch (IntrospectionException exception) {
            throw new CoredeuxStrategyException("Unable to inspect property '" + field + "' on class: "
                    + type.getName(), exception);
        } catch (ReflectiveOperationException exception) {
            throw new CoredeuxStrategyException("Unable to read property '" + field + "' on class: "
                    + type.getName(), exception);
        }

        Field declaredField = getDeclaredField(field, type);
        if (declaredField == null) {
            throw new CoredeuxStrategyException(
                    "Unable to resolve property '" + field + "' on class: " + type.getName());
        }

        try {
            declaredField.setAccessible(true);
            return declaredField.get(instance);
        } catch (IllegalAccessException exception) {
            throw new CoredeuxStrategyException("Unable to read field '" + field + "' on class: " + type.getName(),
                    exception);
        }
    }

    @Override
    public void setFieldValue(Field field, Object instance, Object value) {
        if (field == null) {
            throw new CoredeuxStrategyException("Field must not be null");
        }

        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException exception) {
            throw new CoredeuxStrategyException(
                    "Unable to set field '" + field.getName() + "' on class: " + field.getDeclaringClass().getName(),
                    exception);
        }
    }

    @Override
    public Field getDeclaredField(String field, Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(field);
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Override
    public List<Field> getAllFields(Class<?> type) {
        if (type == null) {
            return List.of();
        }

        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return List.copyOf(fields);
    }
}
