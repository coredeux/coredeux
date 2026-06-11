package com.coredeux.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Demo-specific settings that control how entity definitions are seeded,
 * cached, and refreshed.
 */
@Component
@ConfigurationProperties(prefix = "coredeux.demo.entity-definitions")
public class DemoEntityDefinitionProperties {

    private String registryCode = "coredeux-demo";
    private String bootstrapLocation = "classpath:coredeux-entities.yml";
    private String redisKey = "coredeux:demo:entity-definitions";
    private boolean bootstrapEnabled = true;

    public String getRegistryCode() {
        return registryCode;
    }

    public void setRegistryCode(String registryCode) {
        this.registryCode = registryCode;
    }

    public String getBootstrapLocation() {
        return bootstrapLocation;
    }

    public void setBootstrapLocation(String bootstrapLocation) {
        this.bootstrapLocation = bootstrapLocation;
    }

    public String getRedisKey() {
        return redisKey;
    }

    public void setRedisKey(String redisKey) {
        this.redisKey = redisKey;
    }

    public boolean isBootstrapEnabled() {
        return bootstrapEnabled;
    }

    public void setBootstrapEnabled(boolean bootstrapEnabled) {
        this.bootstrapEnabled = bootstrapEnabled;
    }
}
