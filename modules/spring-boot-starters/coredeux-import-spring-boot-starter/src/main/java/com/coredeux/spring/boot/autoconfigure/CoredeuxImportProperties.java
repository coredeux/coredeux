package com.coredeux.spring.boot.autoconfigure;

import org.springframework.core.env.Environment;

import com.coredeux.core.config.CoredeuxProperties;

public final class CoredeuxImportProperties {

    private final CoredeuxProperties coredeuxProperties;
    private final Environment environment;

    CoredeuxImportProperties(CoredeuxProperties coredeuxProperties, Environment environment) {
        this.coredeuxProperties = coredeuxProperties == null ? new CoredeuxProperties() : coredeuxProperties;
        this.environment = environment;
    }

    public String defaultParser() {
        return string("default-parser", "text");
    }

    String string(String path, String defaultValue) {
        String environmentValue = environmentValue("coredeux.import." + path);
        if (environmentValue != null) {
            return environmentValue;
        }
        String configuredValue = coredeuxProperties.string("import." + path);
        return configuredValue == null || configuredValue.isBlank() ? defaultValue : configuredValue.trim();
    }

    private String environmentValue(String propertyName) {
        if (environment == null || propertyName == null || propertyName.isBlank()) {
            return null;
        }
        String value = environment.getProperty(propertyName);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
