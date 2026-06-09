package com.coredeux.drl.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

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
    void fallsBackToExistingSystemPropertiesWhenNoCoredeuxOverrideExists() {
        String previousConfigCompiler = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY);
        String previousConfigLanguageLevel = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY);
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, " ");
            System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, " ");
            System.setProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, "ECJ");
            System.setProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, "17");

            assertEquals("ECJ", DrlRuntimeBootstrap.resolveJavaCompiler());
            assertEquals("17", DrlRuntimeBootstrap.resolveJavaLanguageLevel());
        } finally {
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, previousConfigCompiler);
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, previousConfigLanguageLevel);
            restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, previousCompiler);
            restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, previousLanguageLevel);
        }
    }

    @Test
    void initializesSystemPropertiesOnceAndUsesResolvedValues() throws Exception {
        String previousConfigCompiler = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY);
        String previousConfigLanguageLevel = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY);
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, "ECJ");
            System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, "17");
            clearProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
            clearProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
            resetInitializedFlag();

            DrlRuntimeBootstrap.initialize();
            assertEquals("ECJ", System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY));
            assertEquals("17", System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY));

            System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, "NATIVE");
            System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, "19");
            DrlRuntimeBootstrap.initialize();

            assertEquals("ECJ", System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY));
            assertEquals("17", System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY));
        } finally {
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, previousConfigCompiler);
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, previousConfigLanguageLevel);
            restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, previousCompiler);
            restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, previousLanguageLevel);
            resetInitializedFlag();
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

    @Test
    void rejectsJavaVersionsBelowTheSupportedFloor() {
        assertThrows(IllegalStateException.class, () -> DrlRuntimeBootstrap.resolveJavaLanguageLevel(16));
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

    private void resetInitializedFlag() throws Exception {
        Field field = DrlRuntimeBootstrap.class.getDeclaredField("INITIALIZED");
        field.setAccessible(true);
        AtomicBoolean initialized = (AtomicBoolean) field.get(null);
        initialized.set(false);
    }
}
