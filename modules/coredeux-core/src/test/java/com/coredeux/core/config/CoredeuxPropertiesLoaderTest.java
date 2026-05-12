package com.coredeux.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CoredeuxPropertiesLoaderTest {

    private final CoredeuxPropertiesLoader loader = new CoredeuxPropertiesLoader();

    @Test
    void shouldLoadDefaultMetaInfConfigurationFromClasspath() {
        CoredeuxProperties properties = loader.load();

        assertEquals("classpath:test-entities.yml", properties.string("entities.config-location"));
    }

    @Test
    void shouldLoadNestedPropertiesFromInputStream() {
                String yaml = """
                coredeux:
                  entities:
                    config-location: classpath:sample-entities.yml
                  custom:
                    feature-flags:
                      - alpha
                      - beta
                """;

        CoredeuxProperties properties = loader.load(inputStream(yaml));

        assertEquals("classpath:sample-entities.yml", properties.string("entities.config-location"));
        assertEquals("alpha", ((java.util.List<?>) properties.value("custom.feature-flags")).get(0));
        assertEquals("beta", ((java.util.List<?>) properties.value("custom.feature-flags")).get(1));
    }

    @Test
    void shouldLoadFromPath() throws Exception {
        Path tempFile = Files.createTempFile("coredeux-properties", ".yml");
        Files.writeString(tempFile,
                """
                        coredeux:
                          export:
                            default-format: JSON
                        """,
                StandardCharsets.UTF_8);

        try {
            CoredeuxProperties properties = loader.load(tempFile);

            assertEquals("JSON", properties.string("export.default-format"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldReturnEmptyPropertiesWhenInputStreamIsNull() {
        CoredeuxProperties properties = loader.load((java.io.InputStream) null);

        assertTrue(properties.asMap().isEmpty());
        assertNull(properties.value("missing.path"));
    }

    @Test
    void shouldExposeMergedOverridesWithoutChangingOriginalValues() {
        CoredeuxProperties base = loader.load(inputStream("""
                coredeux:
                  custom:
                    environment: test
                """));

        CoredeuxProperties overridden = base.withOverrides(Map.of(
                "custom", Map.of("environment", "local")));

        assertEquals("test", base.string("custom.environment"));
        assertEquals("local", overridden.string("custom.environment"));
    }

    private ByteArrayInputStream inputStream(String yaml) {
        return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    }
}
