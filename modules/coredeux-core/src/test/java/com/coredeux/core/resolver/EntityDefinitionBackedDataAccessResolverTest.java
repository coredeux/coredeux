package com.coredeux.core.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;

class EntityDefinitionBackedDataAccessResolverTest {

    private final EntityDefinitionBackedDataAccessResolver resolver = new EntityDefinitionBackedDataAccessResolver();

    @Test
    void shouldResolveConfiguredDataAccessServiceName() {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName("com.example.customer.Customer")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder()
                        .store("postgres")
                        .dataAccessService("postgresCustomerDataAccess")
                        .build())
                .build();

        assertEquals("postgresCustomerDataAccess", resolver.resolveDataAccessService(definition));
    }

    @Test
    void shouldFailWhenDataAccessServiceIsMissing() {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName("com.example.customer.Customer")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder()
                        .store("postgres")
                        .build())
                .build();

        assertThrows(CoredeuxValidationException.class, () -> resolver.resolveDataAccessService(definition));
    }
}
