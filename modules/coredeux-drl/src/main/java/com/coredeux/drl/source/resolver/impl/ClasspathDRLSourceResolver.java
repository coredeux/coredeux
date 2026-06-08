package com.coredeux.drl.source.resolver.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.coredeux.drl.source.resolver.DRLSourceResolver;

/**
 * Resolves DRL files from the classpath using a rule id as the resource name.
 *
 * <p>By default, the resolver looks for {@code <ruleId>.drl} on the current
 * thread context class loader.
 */
public class ClasspathDRLSourceResolver implements DRLSourceResolver {

    private static final String DEFAULT_PREFIX = "";
    private static final String DEFAULT_SUFFIX = ".drl";

    private final ClassLoader classLoader;
    private final String prefix;
    private final String suffix;

    /**
     * Creates a classpath resolver with default resource naming and the current
     * thread context class loader.
     */
    public ClasspathDRLSourceResolver() {
        this(Thread.currentThread().getContextClassLoader(), DEFAULT_PREFIX, DEFAULT_SUFFIX);
    }

    /**
     * Creates a classpath resolver with a custom class loader and default
     * resource naming.
     *
     * @param classLoader class loader used to load DRL resources
     */
    public ClasspathDRLSourceResolver(ClassLoader classLoader) {
        this(classLoader, DEFAULT_PREFIX, DEFAULT_SUFFIX);
    }

    /**
     * Creates a classpath resolver with fully customized resource naming.
     *
     * @param classLoader class loader used to load DRL resources
     * @param prefix optional resource path prefix
     * @param suffix optional resource path suffix
     */
    public ClasspathDRLSourceResolver(ClassLoader classLoader, String prefix, String suffix) {
        this.classLoader = classLoader == null ? ClasspathDRLSourceResolver.class.getClassLoader() : classLoader;
        this.prefix = prefix == null ? DEFAULT_PREFIX : prefix;
        this.suffix = suffix == null ? DEFAULT_SUFFIX : suffix;
    }

    @Override
    /**
     * Loads the DRL text for the given rule identifier from the classpath.
     *
     * @param ruleId the rule identifier, usually mapped to a {@code .drl} file
     * @return the DRL source text
     * @throws IllegalStateException if the resource cannot be found or read
     */
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
