package com.coredeux.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.definition.CoredeuxYamlConfiguration;
import com.coredeux.core.loader.EntityDefinitionLoader;

class EntityDefinitionRegistriesTest {

    @Test
    void shouldBuildRegistryFromPathUsingProvidedLoader() throws IOException {
        Path tempFile = Files.createTempFile("coredeux-entities", ".yml");
        Files.writeString(tempFile, "ignored", StandardCharsets.UTF_8);

        try {
            RecordingLoader loader = new RecordingLoader();

            EntityDefinitionRegistry registry = EntityDefinitionRegistries.fromYaml(tempFile, loader);

            assertNotNull(registry);
            assertTrue(loader.pathInvoked);
            assertEquals(tempFile, loader.loadedPath);
            assertTrue(registry.findByFullClassName("com.example.Customer").isPresent());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldBuildRegistryFromPathWithDefaultLoader() throws IOException {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.Customer
                      identifier: id
                      storage:
                        data-access-service: customerDataAccess
                """;
        Path tempFile = Files.createTempFile("coredeux-default-entities", ".yml");
        Files.writeString(tempFile, yaml, StandardCharsets.UTF_8);

        try {
            EntityDefinitionRegistry registry = EntityDefinitionRegistries.fromYaml(tempFile);

            assertTrue(registry.findByFullClassName("com.example.Customer").isPresent());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldBuildRegistryFromInputStreamUsingProvidedLoader() {
        RecordingLoader loader = new RecordingLoader();
        InputStream inputStream = new ByteArrayInputStream("ignored".getBytes(StandardCharsets.UTF_8));

        EntityDefinitionRegistry registry = EntityDefinitionRegistries.fromYaml(inputStream, loader);

        assertNotNull(registry);
        assertTrue(loader.streamInvoked);
        assertSameReference(inputStream, loader.loadedStream);
        assertTrue(registry.findByFullClassName("com.example.Customer").isPresent());
    }

    @Test
    void shouldRejectNullPathAndLoader() {
        RecordingLoader loader = new RecordingLoader();

        NullPointerException nullPath = assertThrows(NullPointerException.class,
                () -> EntityDefinitionRegistries.fromYaml((Path) null, loader));
        NullPointerException nullLoader = assertThrows(NullPointerException.class,
                () -> EntityDefinitionRegistries.fromYaml(Path.of("sample.yml"), null));

        assertEquals("path must not be null", nullPath.getMessage());
        assertEquals("loader must not be null", nullLoader.getMessage());
    }

    @Test
    void shouldRejectNullInputStreamAndLoader() {
        RecordingLoader loader = new RecordingLoader();
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        NullPointerException nullStream = assertThrows(NullPointerException.class,
                () -> EntityDefinitionRegistries.fromYaml((InputStream) null, loader));
        NullPointerException nullLoader = assertThrows(NullPointerException.class,
                () -> EntityDefinitionRegistries.fromYaml(inputStream, null));

        assertEquals("inputStream must not be null", nullStream.getMessage());
        assertEquals("loader must not be null", nullLoader.getMessage());
    }

    @Test
    void shouldBuildRegistryFromInputStreamWithDefaultLoader() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.Customer
                      identifier: id
                      storage:
                        data-access-service: customerDataAccess
                """;

        EntityDefinitionRegistry registry = EntityDefinitionRegistries
                .fromYaml(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        assertTrue(registry.findByFullClassName("com.example.Customer").isPresent());
    }

    private void assertSameReference(Object expected, Object actual) {
        assertTrue(expected == actual);
    }

    private static final class RecordingLoader implements EntityDefinitionLoader {

        private boolean pathInvoked;
        private boolean streamInvoked;
        private Path loadedPath;
        private InputStream loadedStream;

        @Override
        public CoredeuxYamlConfiguration load(Path path) {
            pathInvoked = true;
            loadedPath = path;
            return configuration();
        }

        @Override
        public CoredeuxYamlConfiguration load(InputStream inputStream) {
            streamInvoked = true;
            loadedStream = inputStream;
            return configuration();
        }

        private CoredeuxYamlConfiguration configuration() {
            return CoredeuxYamlConfiguration.builder()
                    .entities(List.of(CoredeuxEntityDefinition.builder()
                            .fullClassName("com.example.Customer")
                            .name("customer")
                            .identifier("id")
                            .storage(CoredeuxStorageDefinition.builder().dataAccessService("customerDataAccess").build())
                            .build()))
                    .build();
        }
    }
}
