package com.coredeux.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;

class InMemoryEntityDefinitionRegistryTest {

    @Test
    void shouldFindDefinitionByFullClassName() {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName("com.example.customer.Customer")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder()
                        .store("postgres")
                        .dataAccessService("postgresCustomerDataAccess")
                        .build())
                .build();

        EntityDefinitionRegistry registry = new InMemoryEntityDefinitionRegistry(List.of(definition));

        assertTrue(registry.findByFullClassName("com.example.customer.Customer").isPresent());
        assertEquals("postgresCustomerDataAccess",
                registry.findByFullClassName("com.example.customer.Customer").orElseThrow().getStorage()
                        .getDataAccessService());
    }

    @Test
    void shouldRejectDuplicateFullClassNames() {
        CoredeuxEntityDefinition first = CoredeuxEntityDefinition.builder()
                .fullClassName("com.example.customer.Customer")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder()
                        .store("postgres")
                        .dataAccessService("postgresCustomerDataAccess")
                        .build())
                .build();
        CoredeuxEntityDefinition duplicate = CoredeuxEntityDefinition.builder()
                .fullClassName("com.example.customer.Customer")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder()
                        .store("mongo")
                        .dataAccessService("mongoCustomerDataAccess")
                        .build())
                .build();

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> new InMemoryEntityDefinitionRegistry(List.of(first, duplicate)));

        assertTrue(exception.getMessage().contains("Duplicate entity definition"));
    }
}
