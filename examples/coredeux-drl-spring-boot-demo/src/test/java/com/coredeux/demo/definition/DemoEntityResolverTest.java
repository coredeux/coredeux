package com.coredeux.demo.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;

@ExtendWith(MockitoExtension.class)
class DemoEntityResolverTest {

    @Mock
    private EntityDefinitionRegistry registry;

    @Test
    void resolvesDefinitionTypeAndIdentifierFromTheRegistry() {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("sampleDataAccess").build())
                .build();
        when(registry.findByFullClassName(definition.getFullClassName())).thenReturn(java.util.Optional.of(definition));

        DemoEntityResolver resolver = new DemoEntityResolver(registry, new DefaultCoredeuxReflectionHelperService());

        assertEquals(definition, resolver.resolveDefinition(definition.getFullClassName()));
        assertEquals(SampleEntity.class, resolver.resolveType(definition.getFullClassName()));
        assertEquals(String.class, resolver.resolveIdentifierType(SampleEntity.class, definition));

        SampleEntity entity = new SampleEntity();
        resolver.applyIdentifier(entity, definition, "42");
        assertEquals("42", entity.getId());
    }

    @Test
    void rejectsBlankAndUnknownEntityNames() {
        DemoEntityResolver resolver = new DemoEntityResolver(registry, new DefaultCoredeuxReflectionHelperService());

        assertThrows(CoredeuxValidationException.class, () -> resolver.resolveDefinition(" "));
        when(registry.findByFullClassName("unknown")).thenReturn(java.util.Optional.empty());
        assertThrows(CoredeuxValidationException.class, () -> resolver.resolveDefinition("unknown"));
    }

    private static final class SampleEntity {

        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
