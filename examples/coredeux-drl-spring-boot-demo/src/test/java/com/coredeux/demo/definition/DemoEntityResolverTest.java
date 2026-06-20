package com.coredeux.demo.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.resolver.CoredeuxEntityDefinitionResolver;

@ExtendWith(MockitoExtension.class)
class DemoEntityResolverTest {

    @Mock
    private CoredeuxEntityDefinitionResolver entityDefinitionResolver;

    @Test
    void resolvesDefinitionTypeAndIdentifierFromTheRegistry() {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("sampleDataAccess").build())
                .build();
        CoredeuxEntityDefinition effectiveDefinition = definition.toBuilder()
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("sampleDataAccess").build())
                .build();
        when(entityDefinitionResolver.resolve(SampleEntity.class)).thenReturn(effectiveDefinition);

        DemoEntityResolver resolver = new DemoEntityResolver(new DefaultCoredeuxReflectionHelperService(),
                entityDefinitionResolver);

        assertEquals(effectiveDefinition, resolver.resolveDefinition(definition.getFullClassName()));
        assertEquals(SampleEntity.class, resolver.resolveType(definition.getFullClassName()));
        assertEquals(String.class, resolver.resolveIdentifierType(SampleEntity.class, definition));

        SampleEntity entity = new SampleEntity();
        resolver.applyIdentifier(entity, definition, "42");
        assertEquals("42", entity.getId());
    }

    @Test
    void rejectsBlankAndUnknownEntityNames() {
        DemoEntityResolver resolver = new DemoEntityResolver(new DefaultCoredeuxReflectionHelperService(),
                entityDefinitionResolver);

        assertThrows(CoredeuxValidationException.class, () -> resolver.resolveDefinition(" "));
        assertThrows(CoredeuxStrategyException.class, () -> resolver.resolveDefinition("unknown"));
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
