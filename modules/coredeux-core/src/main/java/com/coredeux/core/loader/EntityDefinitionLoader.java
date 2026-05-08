package com.coredeux.core.loader;

import java.io.InputStream;
import java.nio.file.Path;

import com.coredeux.core.definition.CoredeuxYamlConfiguration;

/**
 * Loader for entity definitions from external configuration sources.
 */
public interface EntityDefinitionLoader {

    /**
     * Loads Coredeux entity configuration from the given input stream.
     *
     * @param inputStream the YAML input stream
     * @return the parsed Coredeux configuration
     */
    CoredeuxYamlConfiguration load(InputStream inputStream);

    /**
     * Loads Coredeux entity configuration from the given file path.
     *
     * @param path the YAML file path
     * @return the parsed Coredeux configuration
     */
    CoredeuxYamlConfiguration load(Path path);
}
