package com.coredeux.drl.config;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resolves and applies the Drools Java dialect settings used by the runtime.
 *
 * <p>The bootstrap reads Coredeux-specific overrides first, then existing JVM
 * system properties, and finally the runtime Java feature version before
 * applying the resolved values once at startup.
 */
public final class DrlRuntimeBootstrap {

    /**
     * System property used by Drools to select the Java dialect compiler.
     */
    public static final String JAVA_COMPILER_PROPERTY = "drools.dialect.java.compiler";
    /**
     * System property used by Drools to select the Java language level.
     */
    public static final String JAVA_LANGUAGE_LEVEL_PROPERTY = "drools.dialect.java.compiler.lnglevel";
    /**
     * Coredeux override property for the Drools Java compiler.
     */
    public static final String CONFIG_JAVA_COMPILER_PROPERTY = "coredeux.drl.java-compiler";
    /**
     * Coredeux override property for the Drools Java language level.
     */
    public static final String CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY = "coredeux.drl.java-language-level";
    /**
     * Default Java compiler used when no override is supplied.
     */
    public static final String DEFAULT_JAVA_COMPILER = "NATIVE";
    /**
     * Highest verified Java language level used as the default ceiling when no
     * explicit override is supplied.
     */
    public static final String DEFAULT_JAVA_LANGUAGE_LEVEL = "19";

    private static final int MIN_SUPPORTED_JAVA_VERSION = 17;

    private static final NavigableMap<Integer, String> JAVA_TO_DRL_LANGUAGE_LEVEL = new TreeMap<>();

    static {
        JAVA_TO_DRL_LANGUAGE_LEVEL.put(17, "17");
        JAVA_TO_DRL_LANGUAGE_LEVEL.put(19, "19");
    }

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private DrlRuntimeBootstrap() {
    }

    /**
     * Applies the resolved Drools Java dialect settings once per JVM startup.
     *
     * <p>Subsequent calls are ignored so the runtime does not repeatedly mutate
     * global system properties during rule execution.
     */
    public static void initialize() {
        if (INITIALIZED.compareAndSet(false, true)) {
            String javaCompiler = resolveJavaCompiler();
            String javaLanguageLevel = resolveJavaLanguageLevel();

            System.setProperty(JAVA_COMPILER_PROPERTY, javaCompiler);
            System.setProperty(JAVA_LANGUAGE_LEVEL_PROPERTY, javaLanguageLevel);
        }
    }

    /**
     * Resolves the Java compiler setting from Coredeux overrides, existing
     * Drools system properties, or the built-in default.
     *
     * @return the effective Java compiler setting
     */
    public static String resolveJavaCompiler() {
        return firstNonBlank(System.getProperty(CONFIG_JAVA_COMPILER_PROPERTY),
                System.getProperty(JAVA_COMPILER_PROPERTY), DEFAULT_JAVA_COMPILER);
    }

    /**
     * Resolves the Java language level from Coredeux overrides, existing
     * Drools system properties, or the current runtime Java version.
     *
     * @return the effective Java language level
     */
    public static String resolveJavaLanguageLevel() {
        String configuredLanguageLevel = firstNonBlank(System.getProperty(CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY),
                System.getProperty(JAVA_LANGUAGE_LEVEL_PROPERTY));
        if (configuredLanguageLevel != null) {
            return configuredLanguageLevel;
        }
        return resolveJavaLanguageLevel(Runtime.version().feature());
    }

    /**
     * Resolves the preferred Java language level for a specific Java feature
     * version.
     *
     * <p>The current map is intentionally small: Java 17 and 18 fall back to
     * DRL 17, while Java 19 and above use DRL 19. This keeps the runtime safe
     * on Java 17+ while still using the highest verified Drools level.
     *
     * @param javaFeatureVersion the detected Java feature version
     * @return the effective DRL language level for that Java version
     */
    static String resolveJavaLanguageLevel(int javaFeatureVersion) {
        if (javaFeatureVersion < MIN_SUPPORTED_JAVA_VERSION) {
            throw new IllegalStateException("Coredeux DRL requires Java 17 or later.");
        }

        return JAVA_TO_DRL_LANGUAGE_LEVEL.floorEntry(javaFeatureVersion).getValue();
    }

    /**
     * Returns the first non-blank value from the supplied candidates.
     *
     * @param values candidate values in priority order
     * @return the first non-blank value, or {@code null} if none is present
     */
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
