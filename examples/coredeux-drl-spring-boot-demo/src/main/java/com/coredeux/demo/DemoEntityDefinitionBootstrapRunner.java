package com.coredeux.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.coredeux.demo.definition.DatabaseBackedEntityDefinitionRegistry;

@Component
@Order(0)
@ConditionalOnProperty(value = "coredeux.demo.entity-definitions.bootstrap-enabled", havingValue = "true", matchIfMissing = true)
public class DemoEntityDefinitionBootstrapRunner implements CommandLineRunner {

    private final DatabaseBackedEntityDefinitionRegistry registry;

    public DemoEntityDefinitionBootstrapRunner(DatabaseBackedEntityDefinitionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run(String... args) {
        registry.bootstrapFromFile();
    }
}
