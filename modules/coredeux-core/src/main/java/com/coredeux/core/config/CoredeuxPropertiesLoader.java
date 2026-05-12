package com.coredeux.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Loads the Coredeux runtime configuration from YAML.
 *
 * <p>
 * The loader is intentionally generic. It reads the YAML into a nested map and
 * leaves module-specific interpretation to the module that owns the key.
 * </p>
 */
public final class CoredeuxPropertiesLoader {

    public static final String DEFAULT_LOCATION = "META-INF/coredeux.yml";

    public CoredeuxProperties load() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = CoredeuxPropertiesLoader.class.getClassLoader();
        }

        try (InputStream inputStream = classLoader.getResourceAsStream(DEFAULT_LOCATION)) {
            if (inputStream == null) {
                return new CoredeuxProperties();
            }
            return load(inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load Coredeux properties from " + DEFAULT_LOCATION, exception);
        }
    }

    public CoredeuxProperties load(Path path) {
        if (path == null) {
            return new CoredeuxProperties();
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            return load(inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load Coredeux properties from path: " + path, exception);
        }
    }

    public CoredeuxProperties load(InputStream inputStream) {
        if (inputStream == null) {
            return new CoredeuxProperties();
        }

        Object parsed = new Yaml(new SafeConstructor(new LoaderOptions())).load(inputStream);
        if (!(parsed instanceof Map<?, ?> rootMap)) {
            return new CoredeuxProperties();
        }
        return new CoredeuxProperties(coredeuxSection(rootMap));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> coredeuxSection(Map<?, ?> rootMap) {
        Object candidate = rootMap.get("coredeux");
        if (candidate instanceof Map<?, ?> map) {
            return copyMap((Map<String, Object>) map);
        }
        return copyMap((Map<String, Object>) rootMap);
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), copyValue(entry.getValue()));
        }
        return copy;
    }

    private Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                nested.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
            }
            return nested;
        }

        if (value instanceof Iterable<?> iterable) {
            java.util.List<Object> nested = new java.util.ArrayList<>();
            for (Object item : iterable) {
                nested.add(copyValue(item));
            }
            return nested;
        }

        return value;
    }
}
