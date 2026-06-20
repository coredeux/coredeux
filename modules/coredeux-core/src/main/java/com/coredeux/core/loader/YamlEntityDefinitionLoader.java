package com.coredeux.core.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.coredeux.core.definition.CoredeuxAttributeDefinition;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.definition.CoredeuxYamlConfiguration;
import com.coredeux.core.exceptions.CoredeuxValidationException;

/**
 * SnakeYAML-based loader for Coredeux entity definitions.
 */
public class YamlEntityDefinitionLoader implements EntityDefinitionLoader {

    private static final String ROOT_KEY = "coredeux";
    private static final String ENTITIES_KEY = "entities";
    private static final String MODULES_KEY = "modules";

    @Override
    public CoredeuxYamlConfiguration load(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        Map<String, Object> document = readDocument(inputStream);
        Map<String, Object> coredeux = asMap(document.get(ROOT_KEY), ROOT_KEY);
        List<Map<String, Object>> entities = asMapList(coredeux.get(ENTITIES_KEY), ENTITIES_KEY);

        List<CoredeuxEntityDefinition> definitions = new ArrayList<>();
        for (Map<String, Object> entityMap : entities) {
            definitions.add(toEntityDefinition(entityMap));
        }

        return CoredeuxYamlConfiguration.builder()
                .entities(Collections.unmodifiableList(definitions))
                .build();
    }

    @Override
    public CoredeuxYamlConfiguration load(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream inputStream = Files.newInputStream(path)) {
            return load(inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Coredeux configuration from path: " + path, exception);
        }
    }

    private Map<String, Object> readDocument(InputStream inputStream) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object loaded = yaml.load(inputStream);
        if (loaded == null) {
            throw new CoredeuxValidationException("Coredeux YAML configuration is empty");
        }
        return asMap(loaded, "root");
    }

    private CoredeuxEntityDefinition toEntityDefinition(Map<String, Object> entityMap) {
        String fullClassName = requiredString(entityMap, "full-class-name");
        String name = optionalString(entityMap, "name");
        Map<String, Object> storageMap = optionalMap(entityMap.get("storage"), "storage");

        return CoredeuxEntityDefinition.builder()
                .fullClassName(fullClassName)
                .name(name == null || name.isBlank() ? fullClassName : name)
                .identifier(optionalString(entityMap, "identifier"))
                .storage(storageMap.isEmpty() ? null : toStorageDefinition(storageMap))
                .modules(asEntityModules(entityMap.get(MODULES_KEY)))
                .build();
    }

    private CoredeuxStorageDefinition toStorageDefinition(Map<String, Object> storageMap) {
        return CoredeuxStorageDefinition.builder()
                .store(optionalString(storageMap, "store"))
                .identifier(optionalString(storageMap, "identifier"))
                .dataAccessService(optionalString(storageMap, "data-access-service"))
                .build();
    }

    private List<CoredeuxModuleDefinition> asEntityModules(Object value) {
        if (value == null) {
            return List.of();
        }

        List<Map<String, Object>> moduleMaps = asMapList(value, MODULES_KEY);
        List<CoredeuxModuleDefinition> modules = new ArrayList<>();
        for (Map<String, Object> moduleMap : moduleMaps) {
            String name = requiredString(moduleMap, "name");
            modules.add(CoredeuxModuleDefinition.builder()
                    .name(name)
                    .enabled(!moduleMap.containsKey("enabled") || Boolean.TRUE.equals(moduleMap.get("enabled")))
                    .handlers(asStringList(moduleMap.get("handlers")))
                    .config(convertModuleConfig(name, moduleMap.get("config")))
                    .build());
        }
        return Collections.unmodifiableList(modules);
    }

    private Object convertModuleConfig(String moduleName, Object value) {
        if (value == null) {
            return null;
        }

        switch (moduleName) {
            case "attributes":
                return asAttributes(value);
            default:
                return copyValue(value);
        }
    }

    private List<CoredeuxAttributeDefinition> asAttributes(Object value) {
        List<Map<String, Object>> attributeMaps = asMapList(value, "attributes");
        List<CoredeuxAttributeDefinition> attributes = new ArrayList<>();
        for (Map<String, Object> attributeMap : attributeMaps) {
            attributes.add(CoredeuxAttributeDefinition.builder()
                    .name(requiredString(attributeMap, "name"))
                    .type(requiredString(attributeMap, "type"))
                    .required(Boolean.TRUE.equals(attributeMap.get("required")))
                    .searchable(Boolean.TRUE.equals(attributeMap.get("searchable")))
                    .validators(asStringList(attributeMap.get("validators")))
                    .build());
        }
        return Collections.unmodifiableList(attributes);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value, String fieldName) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new CoredeuxValidationException("Expected a map for field: " + fieldName);
        }
        return (Map<String, Object>) rawMap;
    }

    private Map<String, Object> requiredMap(Object value, String fieldName, String missingField) {
        if (value == null) {
            throw new CoredeuxValidationException("Missing required field: " + missingField);
        }
        return asMap(value, fieldName);
    }

    private Map<String, Object> optionalMap(Object value, String fieldName) {
        if (value == null) {
            return Map.of();
        }
        return asMap(value, fieldName);
    }

    private List<Map<String, Object>> asMapList(Object value, String fieldName) {
        if (!(value instanceof List<?> rawList)) {
            throw new CoredeuxValidationException("Expected a list for field: " + fieldName);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawList) {
            result.add(asMap(item, fieldName));
        }
        return result;
    }

    private List<String> asStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawList)) {
            throw new CoredeuxValidationException("Expected a list of strings");
        }

        List<String> strings = new ArrayList<>();
        for (Object item : rawList) {
            strings.add(String.valueOf(item));
        }
        return Collections.unmodifiableList(strings);
    }

    @SuppressWarnings("unchecked")
    private Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                nested.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(nested);
        }

        if (value instanceof List<?> list) {
            List<Object> nested = new ArrayList<>();
            for (Object item : list) {
                nested.add(copyValue(item));
            }
            return Collections.unmodifiableList(nested);
        }

        return value;
    }

    private String requiredString(Map<String, Object> map, String key) {
        String value = optionalString(map, key);
        if (value == null || value.isBlank()) {
            throw new CoredeuxValidationException("Missing required field: " + key);
        }
        return value;
    }

    private String optionalString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
