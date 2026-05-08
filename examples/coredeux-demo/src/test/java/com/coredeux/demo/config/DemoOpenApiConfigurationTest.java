package com.coredeux.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;

class DemoOpenApiConfigurationTest {

    @Test
    void shouldCreateOpenApiMetadata() {
        OpenAPI openAPI = new DemoOpenApiConfiguration().coredeuxDemoOpenApi();

        assertNotNull(openAPI.getInfo());
        assertEquals("Coredeux Demo API", openAPI.getInfo().getTitle());
        assertEquals("0.1.0-SNAPSHOT", openAPI.getInfo().getVersion());
        assertNotNull(openAPI.getInfo().getContact());
        assertEquals("Coredeux", openAPI.getInfo().getContact().getName());
    }
}
