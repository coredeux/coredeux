package com.coredeux.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class CoredeuxDemoApplicationTest {

    @Test
    void shouldLaunchApplicationEntryPoint() {
        String[] args = {
                "--spring.main.web-application-type=none",
                "--spring.main.lazy-initialization=true",
                "--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration,"
                        + "com.coredeux.spring.boot.autoconfigure.CoredeuxExportAutoConfiguration"
        };
        String previousBootstrapEnabled = System.getProperty("coredeux.demo.bootstrap.enabled");
        System.setProperty("coredeux.demo.bootstrap.enabled", "false");
        try {
            assertDoesNotThrow(() -> CoredeuxDemoApplication.main(args));
        } finally {
            if (previousBootstrapEnabled == null) {
                System.clearProperty("coredeux.demo.bootstrap.enabled");
            } else {
                System.setProperty("coredeux.demo.bootstrap.enabled", previousBootstrapEnabled);
            }
        }
    }
}
