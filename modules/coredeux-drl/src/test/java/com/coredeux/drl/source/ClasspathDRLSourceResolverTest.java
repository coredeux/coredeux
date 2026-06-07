package com.coredeux.drl.source;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
