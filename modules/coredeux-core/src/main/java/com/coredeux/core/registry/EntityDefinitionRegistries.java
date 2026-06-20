package com.coredeux.core.registry;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.coredeux.core.loader.EntityDefinitionLoader;
import com.coredeux.core.loader.YamlEntityDefinitionLoader;

/**
 * Convenience factory for building entity definition registries from YAML
 * configuration sources.
 */
public final class EntityDefinitionRegistries {

    private EntityDefinitionRegistries() {
    }

    /**
     * Builds a registry from the given YAML file path.
     *
     * @param path the YAML file path
     * @return the populated entity definition registry
     */
    public static EntityDefinitionRegistry fromYaml(Path path) {
        return fromYaml(path, new YamlEntityDefinitionLoader());
    }

    /**
     * Builds a registry from the given YAML input stream.
     *
     * @param inputStream the YAML input stream
     * @return the populated entity definition registry
     */
    public static EntityDefinitionRegistry fromYaml(InputStream inputStream) {
        return fromYaml(inputStream, new YamlEntityDefinitionLoader());
    }

    /**
     * Builds a registry from the given YAML file path using the provided loader.
     *
     * @param path the YAML file path
     * @param loader the entity definition loader
     * @return the populated entity definition registry
     */
    public static EntityDefinitionRegistry fromYaml(Path path, EntityDefinitionLoader loader) {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        return new InMemoryEntityDefinitionRegistry(loader.load(path).getEntities());
    }

    /**
     * Builds a registry from the given YAML input stream using the provided
     * loader.
     *
     * @param inputStream the YAML input stream
     * @param loader the entity definition loader
     * @return the populated entity definition registry
     */
    public static EntityDefinitionRegistry fromYaml(InputStream inputStream, EntityDefinitionLoader loader) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        return new InMemoryEntityDefinitionRegistry(loader.load(inputStream).getEntities());
    }

    /**
     * Builds an empty registry.
     *
     * @return an empty entity definition registry
     */
    public static EntityDefinitionRegistry empty() {
        return new InMemoryEntityDefinitionRegistry(List.of());
    }
}
