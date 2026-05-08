package com.coredeux.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxYamlConfiguration;
import com.coredeux.core.loader.EntityDefinitionLoader;
import com.coredeux.core.registry.EntityDefinitionRegistry;

class CoredeuxEntityDefinitionConfigurationTest {

    private final CoredeuxEntityDefinitionConfiguration configuration = new CoredeuxEntityDefinitionConfiguration();

    @Test
    void shouldCreateRegistryFromResource() {
        Resource resource = new StringResource("""
                coredeux:
                  entities:
                    - full-class-name: com.example.Customer
                      identifier: id
                      storage:
                        data-access-service: customerDataAccess
                """);

        EntityDefinitionLoader loader = new StubEntityDefinitionLoader(CoredeuxYamlConfiguration.builder()
                .entities(List.of(CoredeuxEntityDefinition.builder()
                        .fullClassName("com.example.Customer")
                        .name("com.example.Customer")
                        .identifier("id")
                        .build()))
                .build());

        EntityDefinitionRegistry registry = configuration.entityDefinitionRegistry(resource, loader);

        assertEquals(1, registry.getAll().size());
    }

    @Test
    void shouldWrapResourceReadFailures() {
        Resource resource = new AbstractResource() {
            @Override
            public String getDescription() {
                return "failing-resource";
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("boom");
            }
        };

        EntityDefinitionLoader loader = new StubEntityDefinitionLoader(
                CoredeuxYamlConfiguration.builder().entities(List.of()).build());

        assertThrows(UncheckedIOException.class, () -> configuration.entityDefinitionRegistry(resource, loader));
    }

    private static final class StubEntityDefinitionLoader implements EntityDefinitionLoader {

        private final CoredeuxYamlConfiguration configuration;

        private StubEntityDefinitionLoader(CoredeuxYamlConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public CoredeuxYamlConfiguration load(InputStream inputStream) {
            return configuration;
        }

        @Override
        public CoredeuxYamlConfiguration load(Path path) {
            return configuration;
        }
    }

    private static final class StringResource extends AbstractResource {

        private final String content;

        private StringResource(String content) {
            this.content = content;
        }

        @Override
        public String getDescription() {
            return "string-resource";
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
