package com.coredeux.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.coredeux.core.loader.EntityDefinitionLoader;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.registry.InMemoryEntityDefinitionRegistry;

/**
 * Spring configuration that exposes the Coredeux entity definition registry as
 * an injectable bean backed by a YAML resource.
 */
@Configuration
public class CoredeuxEntityDefinitionConfiguration {

    /**
     * Creates the entity definition registry from the configured YAML resource.
     *
     * @param resource the YAML resource containing Coredeux entity definitions
     * @param loader the YAML entity definition loader
     * @return the populated entity definition registry
     */
    @Bean
    public EntityDefinitionRegistry entityDefinitionRegistry(
            @Value("${coredeux.entities.config-location:classpath:coredeux-entities.yml}") Resource resource,
            EntityDefinitionLoader loader) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new InMemoryEntityDefinitionRegistry(loader.load(inputStream).getEntities());
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to load Coredeux entity definitions from resource: " + resource, exception);
        }
    }
}
