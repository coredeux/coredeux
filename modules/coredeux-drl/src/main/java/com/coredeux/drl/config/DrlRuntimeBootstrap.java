package com.coredeux.drl.config;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DrlRuntimeBootstrap {

    public static final String JAVA_COMPILER_PROPERTY = "drools.dialect.java.compiler";
    public static final String JAVA_LANGUAGE_LEVEL_PROPERTY = "drools.dialect.java.compiler.lnglevel";
    public static final String CONFIG_JAVA_COMPILER_PROPERTY = "coredeux.drl.java-compiler";
    public static final String CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY = "coredeux.drl.java-language-level";
    public static final String DEFAULT_JAVA_COMPILER = "NATIVE";
    public static final String DEFAULT_JAVA_LANGUAGE_LEVEL = "19";

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private DrlRuntimeBootstrap() {
    }

    public static void initialize() {
        if (INITIALIZED.compareAndSet(false, true)) {
            String javaCompiler = resolveJavaCompiler();
            String javaLanguageLevel = resolveJavaLanguageLevel();

            System.setProperty(JAVA_COMPILER_PROPERTY, javaCompiler);
            System.setProperty(JAVA_LANGUAGE_LEVEL_PROPERTY, javaLanguageLevel);
        }
    }

    public static String resolveJavaCompiler() {
        return firstNonBlank(System.getProperty(CONFIG_JAVA_COMPILER_PROPERTY),
                System.getProperty(JAVA_COMPILER_PROPERTY), DEFAULT_JAVA_COMPILER);
    }

    public static String resolveJavaLanguageLevel() {
        return firstNonBlank(System.getProperty(CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY),
                System.getProperty(JAVA_LANGUAGE_LEVEL_PROPERTY), DEFAULT_JAVA_LANGUAGE_LEVEL);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
