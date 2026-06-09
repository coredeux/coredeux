package com.coredeux.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CoredeuxPropertiesTest {

    @Test
    void shouldReadNestedValuesAndExposeImmutableCopies() {
        CoredeuxProperties properties = new CoredeuxProperties(Map.of(
                "coredeux", Map.of(
                        "entities", Map.of("config-location", "classpath:entities.yml"),
                        "flags", List.of("alpha", "beta")),
                "plain", "value"));

        assertTrue(properties.contains("coredeux.entities.config-location"));
        assertEquals("classpath:entities.yml", properties.string("coredeux.entities.config-location"));
        assertEquals("value", properties.string("plain"));
        assertNull(properties.value("missing.path"));
        assertNull(properties.map("plain"));
        assertEquals("alpha", ((List<?>) properties.value("coredeux.flags")).get(0));
        assertEquals(Map.of("config-location", "classpath:entities.yml"),
                properties.map("coredeux.entities"));

        Map<String, Object> exported = properties.asMap();
        assertThrowsUnsupported(() -> exported.put("new", "value"));
    }

    @Test
    void shouldMergeOverridesWithoutMutatingOriginalInstance() {
        CoredeuxProperties base = new CoredeuxProperties(Map.of(
                "coredeux", Map.of(
                        "entities", Map.of("config-location", "classpath:entities.yml"),
                        "flags", List.of("alpha", "beta")),
                "mode", "test"));

        CoredeuxProperties overridden = base.withOverrides(Map.of(
                "coredeux", Map.of(
                        "entities", Map.of("config-location", "classpath:overridden.yml"),
                        "flags", List.of("gamma")),
                "mode", "local"));

        assertEquals("classpath:entities.yml", base.string("coredeux.entities.config-location"));
        assertEquals("classpath:overridden.yml", overridden.string("coredeux.entities.config-location"));
        assertEquals("test", base.string("mode"));
        assertEquals("local", overridden.string("mode"));
        assertEquals(List.of("alpha", "beta"), base.value("coredeux.flags"));
        assertEquals(List.of("gamma"), overridden.value("coredeux.flags"));
        assertNotEquals(base, overridden);
        assertEquals(base, new CoredeuxProperties(Map.of(
                "coredeux", Map.of(
                        "entities", Map.of("config-location", "classpath:entities.yml"),
                        "flags", List.of("alpha", "beta")),
                "mode", "test")));
        assertEquals(base.hashCode(), new CoredeuxProperties(Map.of(
                "coredeux", Map.of(
                        "entities", Map.of("config-location", "classpath:entities.yml"),
                        "flags", List.of("alpha", "beta")),
                "mode", "test")).hashCode());
        assertTrue(base.toString().contains("CoredeuxProperties"));
        assertFalse(base.contains("missing.path"));
        assertNull(base.value(null));
        assertNull(base.value(" "));
    }

    @Test
    void shouldHandleEmptyAndNullConstructionGracefully() {
        CoredeuxProperties empty = new CoredeuxProperties(null);
        assertTrue(empty.asMap().isEmpty());
        assertEquals(empty, new CoredeuxProperties());
        assertEquals(empty.hashCode(), new CoredeuxProperties().hashCode());
    }

    @SuppressWarnings("unchecked")
    private void assertThrowsUnsupported(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (UnsupportedOperationException expected) {
            return;
        } catch (Exception exception) {
            throw new AssertionError("Unexpected exception", exception);
        }
        throw new AssertionError("Expected UnsupportedOperationException");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
