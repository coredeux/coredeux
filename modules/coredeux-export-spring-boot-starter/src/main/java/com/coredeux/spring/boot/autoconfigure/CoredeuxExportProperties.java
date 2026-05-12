package com.coredeux.spring.boot.autoconfigure;

import java.util.Locale;

import org.springframework.core.env.Environment;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.export.model.ExportFormat;

public final class CoredeuxExportProperties {

    private final CoredeuxProperties coredeuxProperties;
    private final Environment environment;

    CoredeuxExportProperties(CoredeuxProperties coredeuxProperties, Environment environment) {
        this.coredeuxProperties = coredeuxProperties == null ? new CoredeuxProperties() : coredeuxProperties;
        this.environment = environment;
    }

    String string(String path, String defaultValue) {
        String environmentValue = environmentValue("coredeux.export." + path);
        if (environmentValue != null) {
            return environmentValue;
        }
        String configuredValue = coredeuxProperties.string("export." + path);
        return configuredValue == null || configuredValue.isBlank() ? defaultValue : configuredValue.trim();
    }

    int integer(String path, int defaultValue) {
        String value = string(path, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    boolean booleanValue(String path, boolean defaultValue) {
        String value = string(path, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public int getWorkerDelayMs() {
        return integer("worker.delay-ms", 5000);
    }

    public ExportFormat defaultFormat() {
        String value = string("default-format", ExportFormat.TEXT.name());
        try {
            return ExportFormat.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ExportFormat.TEXT;
        }
    }

    private String environmentValue(String propertyName) {
        if (environment == null || propertyName == null || propertyName.isBlank()) {
            return null;
        }
        String value = environment.getProperty(propertyName);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
