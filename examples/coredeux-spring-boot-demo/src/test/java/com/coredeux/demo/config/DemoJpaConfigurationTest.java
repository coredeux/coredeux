package com.coredeux.demo.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.coredeux.core.jpa.service.impl.PostgresCoredeuxJpaDataAccessService;

class DemoJpaConfigurationTest {

    @Test
    void shouldCreatePostgresJpaDataAccessServiceBean() {
        DemoJpaConfiguration configuration = new DemoJpaConfiguration();

        PostgresCoredeuxJpaDataAccessService service = configuration.postgresCoredeuxJpaDataAccessService();

        assertNotNull(service);
        assertInstanceOf(PostgresCoredeuxJpaDataAccessService.class, service);
    }
}
