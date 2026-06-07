package com.coredeux.drl.source.resolver.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.coredeux.drl.source.resolver.DRLSourceResolver;

public class ClasspathDRLSourceResolver implements DRLSourceResolver {

    private static final String DEFAULT_PREFIX = "";
    private static final String DEFAULT_SUFFIX = ".drl";

    private final ClassLoader classLoader;
    private final String prefix;
    private final String suffix;

    public ClasspathDRLSourceResolver() {
        this(Thread.currentThread().getContextClassLoader(), DEFAULT_PREFIX, DEFAULT_SUFFIX);
    }

    public ClasspathDRLSourceResolver(ClassLoader classLoader) {
        this(classLoader, DEFAULT_PREFIX, DEFAULT_SUFFIX);
    }

    public ClasspathDRLSourceResolver(ClassLoader classLoader, String prefix, String suffix) {
        this.classLoader = classLoader == null ? ClasspathDRLSourceResolver.class.getClassLoader() : classLoader;
        this.prefix = prefix == null ? DEFAULT_PREFIX : prefix;
        this.suffix = suffix == null ? DEFAULT_SUFFIX : suffix;
    }

    @Override
    public String resolve(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }

        String resourceName = prefix + ruleId + suffix;
        try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Unable to resolve classpath DRL resource: " + resourceName);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read classpath DRL resource: " + resourceName, exception);
        }
    }
}
