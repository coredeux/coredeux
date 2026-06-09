package com.coredeux.drl.source.resolver.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

class ClasspathDRLSourceResolverTest {

    @Test
    void shouldResolveResourcesUsingDefaultAndCustomLocations() {
        ClasspathDRLSourceResolver defaultResolver = new ClasspathDRLSourceResolver();
        assertTrue(defaultResolver.resolve("sample-rule").contains("classpath-default"));

        ClasspathDRLSourceResolver customResolver = new ClasspathDRLSourceResolver(
                Thread.currentThread().getContextClassLoader(), "rules/custom/", ".rule");
        assertTrue(customResolver.resolve("sample-rule").contains("classpath-custom"));
    }

    @Test
    void shouldRejectBlankRuleIdsAndMissingResources() {
        ClasspathDRLSourceResolver resolver = new ClasspathDRLSourceResolver();

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(null));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(" "));
        assertThrows(IllegalStateException.class,
                () -> new ClasspathDRLSourceResolver(new EmptyClassLoader(), "rules/", ".drl")
                        .resolve("sample-rule"));
    }

    @Test
    void shouldWrapIoFailuresWhenReadingResources() {
        ClasspathDRLSourceResolver resolver = new ClasspathDRLSourceResolver(new ThrowingClassLoader(), "rules/", ".drl");

        assertThrows(IllegalStateException.class, () -> resolver.resolve("sample-rule"));
    }

    private static final class EmptyClassLoader extends ClassLoader {
        @Override
        public InputStream getResourceAsStream(String name) {
            return null;
        }
    }

    private static final class ThrowingClassLoader extends ClassLoader {
        @Override
        public InputStream getResourceAsStream(String name) {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("boom");
                }
            };
        }
    }
}
