package com.coredeux.core.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Map-backed Coredeux configuration payload.
 *
 * <p>
 * Coredeux core intentionally keeps this generic. The core module can load the
 * YAML file and expose its values, but feature modules decide how to interpret
 * their own keys.
 * </p>
 */
public final class CoredeuxProperties implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> values;

    public CoredeuxProperties() {
        this(Map.of());
    }

    public CoredeuxProperties(Map<String, Object> values) {
        this.values = deepCopyMap(values == null ? Map.of() : values);
    }

    public Map<String, Object> asMap() {
        return values;
    }

    public boolean contains(String path) {
        return value(path) != null;
    }

    public Object value(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        Object current = values;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    public String string(String path) {
        Object value = value(path);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> map(String path) {
        Object value = value(path);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    public CoredeuxProperties withOverrides(Map<String, Object> overrides) {
        return new CoredeuxProperties(merge(values, overrides));
    }

    private Map<String, Object> merge(Map<String, Object> base, Map<String, Object> overrides) {
        Map<String, Object> merged = new LinkedHashMap<>(base);
        if (overrides == null || overrides.isEmpty()) {
            return merged;
        }

        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            String key = entry.getKey();
            Object overrideValue = entry.getValue();
            Object existingValue = merged.get(key);

            if (existingValue instanceof Map<?, ?> existingMap && overrideValue instanceof Map<?, ?> overrideMap) {
                merged.put(key, merge(castMap(existingMap), castMap(overrideMap)));
            } else {
                merged.put(key, deepCopyValue(overrideValue));
            }
        }
        return merged;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), deepCopyValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }

        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object item : list) {
                copy.add(deepCopyValue(item));
            }
            return Collections.unmodifiableList(copy);
        }

        return value;
    }

    @Override
    public String toString() {
        return "CoredeuxProperties" + values;
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CoredeuxProperties that)) {
            return false;
        }
        return Objects.equals(values, that.values);
    }
}
