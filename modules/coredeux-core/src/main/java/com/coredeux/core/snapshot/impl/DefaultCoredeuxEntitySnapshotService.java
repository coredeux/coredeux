package com.coredeux.core.snapshot.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.coredeux.core.snapshot.CoredeuxEntitySnapshotService;

/**
 * Default reflection-based snapshotter for simple bean-style entity graphs.
 */
public class DefaultCoredeuxEntitySnapshotService implements CoredeuxEntitySnapshotService {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T snapshot(T value) {
        return (T) snapshotValue(value);
    }

    private Object snapshotValue(Object value) {
        if (value == null || isImmutable(value.getClass())) {
            return value;
        }
        if (value instanceof Date date) {
            return new Date(date.getTime());
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            return snapshotArray(value);
        }
        if (value instanceof Collection<?> collection) {
            return snapshotCollection(collection);
        }
        if (value instanceof Map<?, ?> map) {
            return snapshotMap(map);
        }
        return snapshotBean(value);
    }

    private Object snapshotArray(Object source) {
        int length = Array.getLength(source);
        Object copy = Array.newInstance(source.getClass().getComponentType(), length);
        for (int index = 0; index < length; index++) {
            Array.set(copy, index, snapshotFieldValue(Array.get(source, index)));
        }
        return copy;
    }

    private Collection<Object> snapshotCollection(Collection<?> source) {
        Collection<Object> copy = newCollection(source);
        for (Object item : source) {
            copy.add(snapshotFieldValue(item));
        }
        return copy;
    }

    private Map<Object, Object> snapshotMap(Map<?, ?> source) {
        Map<Object, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(snapshotFieldValue(entry.getKey()), snapshotFieldValue(entry.getValue()));
        }
        return copy;
    }

    private Collection<Object> newCollection(Collection<?> source) {
        if (source instanceof Set<?>) {
            return new LinkedHashSet<>();
        }
        if (source instanceof Queue<?>) {
            return new ArrayDeque<>();
        }
        return new ArrayList<>();
    }

    private Object snapshotBean(Object source) {
        Object copy = instantiate(source.getClass());
        if (copy == null) {
            return source;
        }
        Class<?> current = source.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                copyField(source, copy, field);
            }
            current = current.getSuperclass();
        }
        return copy;
    }

    private Object instantiate(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private void copyField(Object source, Object copy, Field field) {
        try {
            field.setAccessible(true);
            field.set(copy, snapshotFieldValue(field.get(source)));
        } catch (RuntimeException | IllegalAccessException exception) {
            // If a field cannot be copied, keep the snapshot usable with the fields
            // that were copied successfully.
        }
    }

    private Object snapshotFieldValue(Object value) {
        if (value == null || isImmutable(value.getClass())) {
            return value;
        }
        if (value instanceof Date date) {
            return new Date(date.getTime());
        }
        if (value.getClass().isArray()) {
            return snapshotArray(value);
        }
        if (value instanceof Collection<?> collection) {
            return snapshotCollection(collection);
        }
        if (value instanceof Map<?, ?> map) {
            return snapshotMap(map);
        }
        return value;
    }

    private boolean isImmutable(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || String.class == type
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || BigDecimal.class == type
                || BigInteger.class == type
                || TemporalAccessor.class.isAssignableFrom(type)
                || Class.class == type;
    }
}
