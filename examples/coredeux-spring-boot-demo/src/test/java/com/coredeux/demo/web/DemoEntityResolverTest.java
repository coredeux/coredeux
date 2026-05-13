package com.coredeux.demo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.registry.InMemoryEntityDefinitionRegistry;
import com.coredeux.demo.domain.Customer;

class DemoEntityResolverTest {

    private static final String CUSTOMER_CLASS_NAME = Customer.class.getName();

    @Test
    void shouldResolveByFullClassNameAndIdentifierType() {
        DemoEntityResolver resolver = new DemoEntityResolver(
                new InMemoryEntityDefinitionRegistry(List.of(CoredeuxEntityDefinition.builder()
                        .name("customer")
                        .fullClassName(CUSTOMER_CLASS_NAME)
                        .identifier("pk")
                        .storage(CoredeuxStorageDefinition.builder().dataAccessService("demo").build())
                        .build())),
                new DefaultCoredeuxReflectionHelperService());

        assertEquals(CUSTOMER_CLASS_NAME, resolver.resolveDefinition(CUSTOMER_CLASS_NAME).getFullClassName());
        assertEquals(Customer.class, resolver.resolveType(CUSTOMER_CLASS_NAME));
        assertEquals(Long.class,
                resolver.resolveIdentifierType(Customer.class, resolver.resolveDefinition(CUSTOMER_CLASS_NAME)));
    }

    @Test
    void shouldFailForUnknownOrNonUniqueFriendlyName() {
        DemoEntityResolver resolver = new DemoEntityResolver(
                new InMemoryEntityDefinitionRegistry(List.of(CoredeuxEntityDefinition.builder()
                        .name("customer")
                        .fullClassName(CUSTOMER_CLASS_NAME)
                        .identifier("pk")
                        .storage(CoredeuxStorageDefinition.builder().dataAccessService("demo").build())
                        .build())),
                new DefaultCoredeuxReflectionHelperService());

        assertThrows(CoredeuxValidationException.class, () -> resolver.resolveDefinition("missing"));
        assertThrows(CoredeuxValidationException.class, () -> resolver.resolveDefinition("customer"));
    }
}
