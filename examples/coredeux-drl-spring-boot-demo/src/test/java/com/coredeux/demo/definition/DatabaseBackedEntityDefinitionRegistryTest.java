package com.coredeux.demo.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.registry.EntityDefinitionRegistry;

@ExtendWith(MockitoExtension.class)
class DatabaseBackedEntityDefinitionRegistryTest {

    @Mock
    private EntityDefinitionManager manager;

    @Mock
    private EntityDefinitionRegistry registry;

    @Test
    void delegatesFindByFullClassNameAndGetAllToTheCurrentRegistrySnapshot() {
        DatabaseBackedEntityDefinitionRegistry databaseRegistry = new DatabaseBackedEntityDefinitionRegistry(manager);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName("com.coredeux.demo.domain.Customer")
                .name("customer")
                .identifier("pk")
                .build();

        when(manager.currentRegistry()).thenReturn(registry);
        when(registry.findByFullClassName(definition.getFullClassName())).thenReturn(Optional.of(definition));
        when(registry.getAll()).thenReturn(List.of(definition));

        assertEquals(Optional.of(definition), databaseRegistry.findByFullClassName(definition.getFullClassName()));
        Collection<CoredeuxEntityDefinition> all = databaseRegistry.getAll();

        assertEquals(1, all.size());
        assertSame(definition, all.iterator().next());
        verify(manager, times(2)).currentRegistry();
    }
}
