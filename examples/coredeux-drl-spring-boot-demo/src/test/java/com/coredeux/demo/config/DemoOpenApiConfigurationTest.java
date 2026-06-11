package com.coredeux.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DemoOpenApiConfigurationTest {

    @Test
    void createsAConfiguredOpenApiBean() {
        DemoOpenApiConfiguration configuration = new DemoOpenApiConfiguration();

        assertNotNull(configuration.demoOpenAPI());
        assertEquals("Coredeux DRL Spring Boot Demo", configuration.demoOpenAPI().getInfo().getTitle());
        assertEquals("Small DRL proof-of-concept demo", configuration.demoOpenAPI().getInfo().getDescription());
    }
}
