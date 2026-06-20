package com.coredeux.demo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.resolver.CoredeuxEntityDefinitionResolver;

class DemoEntityResolverTest {

    private final DefaultCoredeuxReflectionHelperService reflectionHelperService = new DefaultCoredeuxReflectionHelperService();
    private final CoredeuxEntityDefinitionResolver entityDefinitionResolver = mock(CoredeuxEntityDefinitionResolver.class);
    private final DemoEntityResolver resolver = new DemoEntityResolver(reflectionHelperService,
            entityDefinitionResolver);

    @Test
    void shouldResolveDefinitionAndType() {
        CoredeuxEntityDefinition definition = mock(CoredeuxEntityDefinition.class);
        when(definition.getFullClassName()).thenReturn("java.lang.String");
        when(entityDefinitionResolver.resolve(String.class)).thenReturn(definition);

        assertEquals(definition, resolver.resolveDefinition("java.lang.String"));
        assertEquals(String.class, resolver.resolveType("java.lang.String"));
    }

    @Test
    void shouldHandleIdentifierLookupAndAssignment() {
        CoredeuxEntityDefinition definition = mock(CoredeuxEntityDefinition.class);
        when(definition.getIdentifier()).thenReturn(null);
        when(definition.getFullClassName()).thenReturn(SampleEntity.class.getName());
        CoredeuxEntityDefinition effectiveDefinition = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("sampleDataAccess").build())
                .build();
        when(entityDefinitionResolver.resolve(SampleEntity.class)).thenReturn(effectiveDefinition);
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
        assertThrows(CoredeuxStrategyException.class, () -> resolver.resolveDefinition("missing"));
        assertThrows(CoredeuxValidationException.class, () -> resolver.applyIdentifier(new Object(), definition, "1"));
        assertThrows(CoredeuxValidationException.class,
                () -> resolver.resolveIdentifierType(Object.class, definition));
    }

    static class SampleEntity {
        String id;
    }
}
