package com.coredeux.demo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;

class DemoEntityResolverTest {

    private final EntityDefinitionRegistry registry = mock(EntityDefinitionRegistry.class);
    private final DefaultCoredeuxReflectionHelperService reflectionHelperService = new DefaultCoredeuxReflectionHelperService();
    private final DemoEntityResolver resolver = new DemoEntityResolver(registry, reflectionHelperService);

    @Test
    void shouldResolveDefinitionAndType() {
        CoredeuxEntityDefinition definition = mock(CoredeuxEntityDefinition.class);
        when(definition.getFullClassName()).thenReturn("java.lang.String");
        when(registry.findByFullClassName("java.lang.String")).thenReturn(java.util.Optional.of(definition));

        assertEquals(definition, resolver.resolveDefinition("java.lang.String"));
        assertEquals(String.class, resolver.resolveType("java.lang.String"));
    }

    @Test
    void shouldHandleIdentifierLookupAndAssignment() {
        CoredeuxEntityDefinition definition = mock(CoredeuxEntityDefinition.class);
        when(definition.getIdentifier()).thenReturn("id");
        SampleEntity entity = new SampleEntity();
        entity.id = "abc";

        assertEquals("abc", resolver.getIdentifierValue(entity, definition));
        resolver.applyIdentifier(entity, definition, "xyz");
        assertEquals("xyz", entity.id);
        assertEquals(String.class, resolver.resolveIdentifierType(SampleEntity.class, definition));
    }

    @Test
    void shouldRejectInvalidRequests() {
        assertThrows(CoredeuxValidationException.class, () -> resolver.resolveDefinition(" "));

        CoredeuxEntityDefinition definition = mock(CoredeuxEntityDefinition.class);
        when(definition.getIdentifier()).thenReturn("id");
        when(registry.findByFullClassName("missing")).thenReturn(java.util.Optional.empty());
        assertThrows(CoredeuxValidationException.class, () -> resolver.resolveDefinition("missing"));
        assertThrows(CoredeuxValidationException.class, () -> resolver.applyIdentifier(new Object(), definition, "1"));
        assertThrows(CoredeuxValidationException.class,
                () -> resolver.resolveIdentifierType(Object.class, definition));
    }

    static class SampleEntity {
        String id;
    }
}
