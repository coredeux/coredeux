package com.coredeux.drl.source;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.coredeux.drl.source.resolver.impl.ClasspathDRLSourceResolver;

class ClasspathDRLSourceResolverTest {

    @Test
    void resolvesDrLFromClasspathUsingRuleIdAsFileName() {
        ClasspathDRLSourceResolver resolver = new ClasspathDRLSourceResolver(
                getClass().getClassLoader(), "rules/", ".drl");

        String drl = resolver.resolve("sample-rule");
        assertTrue(drl.contains("rule \"sample-rule\""));
    }

    @Test
    void supportsDefaultConstructorAndNullClassLoaderFallback() {
        ClasspathDRLSourceResolver defaultResolver = new ClasspathDRLSourceResolver();
        ClasspathDRLSourceResolver nullLoaderResolver = new ClasspathDRLSourceResolver(null, "rules/", ".drl");

        assertTrue(defaultResolver.resolve("sample-rule").contains("rule \"sample-rule\""));
        assertTrue(nullLoaderResolver.resolve("sample-rule").contains("rule \"sample-rule\""));
    }

    @Test
    void fallsBackToDefaultPrefixAndSuffixWhenNullsAreProvided() {
        ClasspathDRLSourceResolver resolver = new ClasspathDRLSourceResolver(getClass().getClassLoader(), null, null);

        assertTrue(resolver.resolve("sample-rule").contains("rule \"sample-rule\""));
    }

    @Test
    void rejectsBlankRuleIdsAndMissingResources() {
        ClasspathDRLSourceResolver resolver = new ClasspathDRLSourceResolver(getClass().getClassLoader(), "rules/", ".drl");

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(" "));
        assertThrows(IllegalStateException.class, () -> resolver.resolve("missing-rule"));
    }
}
