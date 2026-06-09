package com.coredeux.drl.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DrlRuntimeBootstrapTest {

    @Test
    void resolvesDefaultCompilerSettingsWhenNoOverridesAreProvided() {
        String previousConfigCompiler = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY);
        String previousConfigLanguageLevel = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY);
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            clearProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY);
            clearProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY);
            clearProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
            clearProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);

            assertEquals(DrlRuntimeBootstrap.DEFAULT_JAVA_COMPILER, DrlRuntimeBootstrap.resolveJavaCompiler());
            assertEquals(DrlRuntimeBootstrap.resolveJavaLanguageLevel(Runtime.version().feature()),
                    DrlRuntimeBootstrap.resolveJavaLanguageLevel());
        } finally {
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, previousConfigCompiler);
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, previousConfigLanguageLevel);
            restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, previousCompiler);
            restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, previousLanguageLevel);
        }
    }

    @Test
    void resolvesExplicitOverridesBeforeDefaults() {
        String previousConfigCompiler = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY);
        String previousConfigLanguageLevel = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, "NATIVE");
            System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, "19");

            assertEquals("NATIVE", DrlRuntimeBootstrap.resolveJavaCompiler());
            assertEquals("19", DrlRuntimeBootstrap.resolveJavaLanguageLevel());
        } finally {
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, previousConfigCompiler);
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, previousConfigLanguageLevel);
        }
    }

    @Test
    void mapsSupportedJavaVersionsToTheExpectedDrlLevel() {
        assertEquals("17", DrlRuntimeBootstrap.resolveJavaLanguageLevel(17));
        assertEquals("17", DrlRuntimeBootstrap.resolveJavaLanguageLevel(18));
        assertEquals("19", DrlRuntimeBootstrap.resolveJavaLanguageLevel(19));
        assertEquals("19", DrlRuntimeBootstrap.resolveJavaLanguageLevel(21));
        assertEquals("19", DrlRuntimeBootstrap.resolveJavaLanguageLevel(25));
    }

    private void clearProperty(String name) {
        System.clearProperty(name);
    }

    private void restoreProperty(String name, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previousValue);
        }
    }
}
