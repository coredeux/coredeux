package com.coredeux.drl.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DrlRuntimeBootstrapTest {

    private String previousCompiler;
    private String previousLanguageLevel;
    private String previousDroolsCompiler;
    private String previousDroolsLanguageLevel;

    @BeforeEach
    void captureState() {
        previousCompiler = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY);
        previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY);
        previousDroolsCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        previousDroolsLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        resetInitialization();
        clearBootstrapProperties();
    }

    @AfterEach
    void restoreState() {
        restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, previousCompiler);
        restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, previousLanguageLevel);
        restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, previousDroolsCompiler);
        restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, previousDroolsLanguageLevel);
        resetInitialization();
    }

    @Test
    void shouldResolveCompilerAndLanguageLevelsWithOverridesAndFallbacks() {
        assertEquals(DrlRuntimeBootstrap.DEFAULT_JAVA_COMPILER, DrlRuntimeBootstrap.resolveJavaCompiler());
        assertEquals("17", DrlRuntimeBootstrap.resolveJavaLanguageLevel(17));
        assertEquals("17", DrlRuntimeBootstrap.resolveJavaLanguageLevel(18));
        assertEquals("19", DrlRuntimeBootstrap.resolveJavaLanguageLevel(19));
        assertEquals("19", DrlRuntimeBootstrap.resolveJavaLanguageLevel(21));
        assertEquals("19", DrlRuntimeBootstrap.resolveJavaLanguageLevel(25));
        assertThrows(IllegalStateException.class, () -> DrlRuntimeBootstrap.resolveJavaLanguageLevel(16));

        System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, "ECJ");
        System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, "25");

        assertEquals("ECJ", DrlRuntimeBootstrap.resolveJavaCompiler());
        assertEquals("25", DrlRuntimeBootstrap.resolveJavaLanguageLevel());
    }

    @Test
    void shouldInitializeSystemPropertiesOnce() {
        System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, "ECJ");
        System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, "25");

        DrlRuntimeBootstrap.initialize();

        assertEquals("ECJ", System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY));
        assertEquals("25", System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY));

        System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, "NATIVE");
        System.setProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, "19");
        DrlRuntimeBootstrap.initialize();

        assertEquals("ECJ", System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY));
        assertEquals("25", System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY));
    }

    private void clearBootstrapProperties() {
        System.clearProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY);
        System.clearProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY);
        System.clearProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        System.clearProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private void resetInitialization() {
        try {
            Field initialized = DrlRuntimeBootstrap.class.getDeclaredField("INITIALIZED");
            initialized.setAccessible(true);
            ((AtomicBoolean) initialized.get(null)).set(false);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to reset DrlRuntimeBootstrap state", exception);
        }
    }
}
