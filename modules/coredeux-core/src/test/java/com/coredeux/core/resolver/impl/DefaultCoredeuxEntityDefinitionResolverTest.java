package com.coredeux.core.resolver.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.registry.EntityDefinitionRegistry;

class DefaultCoredeuxEntityDefinitionResolverTest {

    @Test
    void shouldSynthesizeDefinitionWhenRegistryHasNoEntry() {
        DefaultCoredeuxEntityDefinitionResolver resolver = new DefaultCoredeuxEntityDefinitionResolver(
                new SimpleRegistry(List.of()),
                new CoredeuxProperties(java.util.Map.of("data-access-service", "globalDataAccess", "identifier",
                        "globalIdentifier")));

        CoredeuxEntityDefinition definition = resolver.resolve(SampleEntity.class);

        assertEquals(SampleEntity.class.getName(), definition.getFullClassName());
        assertEquals("SampleEntity", definition.getName());
        assertEquals("globalIdentifier", definition.getIdentifier());
        assertEquals("globalDataAccess", definition.getStorage().getDataAccessService());
    }

    @Test
    void shouldUseStorageIdentifierWhenEntityIdentifierIsMissing() {
        CoredeuxEntityDefinition configured = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .storage(CoredeuxStorageDefinition.builder().identifier("storageId").build())
                .modules(List.of())
                .build();

        DefaultCoredeuxEntityDefinitionResolver resolver = new DefaultCoredeuxEntityDefinitionResolver(
                new SimpleRegistry(List.of(configured)),
                new CoredeuxProperties(java.util.Map.of("data-access-service", "globalDataAccess")));

        CoredeuxEntityDefinition definition = resolver.resolve(SampleEntity.class);

        assertEquals("sample", definition.getName());
        assertEquals("storageId", definition.getIdentifier());
        assertEquals("globalDataAccess", definition.getStorage().getDataAccessService());
    }

    @Test
    void shouldKeepExplicitEntityIdentifierOverStorageDefault() {
        CoredeuxEntityDefinition configured = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("entityId")
                .storage(CoredeuxStorageDefinition.builder().identifier("storageId").dataAccessService("explicitDataAccess")
                        .build())
                .modules(List.of())
                .build();

        DefaultCoredeuxEntityDefinitionResolver resolver = new DefaultCoredeuxEntityDefinitionResolver(
                new SimpleRegistry(List.of(configured)),
                new CoredeuxProperties(java.util.Map.of("data-access-service", "globalDataAccess")));

        CoredeuxEntityDefinition definition = resolver.resolve(SampleEntity.class);

        assertEquals("explicitDataAccess", definition.getStorage().getDataAccessService());
        assertEquals("entityId", definition.getIdentifier());
        assertEquals(0, definition.getModules().size());
    }

    @Test
    void shouldKeepDefinitionInstanceWhenNormalizedStorageAlreadyMatchesByValue() {
        CoredeuxStorageDefinition storage = CoredeuxStorageDefinition.builder()
                .identifier("storageId")
                .dataAccessService("explicitDataAccess")
                .build();
        CoredeuxEntityDefinition configured = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("entityId")
                .storage(storage)
                .modules(List.of())
                .build();

        DefaultCoredeuxEntityDefinitionResolver resolver = new DefaultCoredeuxEntityDefinitionResolver(
                new SimpleRegistry(List.of(configured)),
                new CoredeuxProperties(java.util.Map.of("data-access-service", "explicitDataAccess")));

        CoredeuxEntityDefinition definition = resolver.resolve(SampleEntity.class);

        assertSame(configured, definition);
    }

    @Test
    void shouldApplyGlobalFallbackWhenConfiguredDefinitionOmitsStorageService() {
        CoredeuxEntityDefinition configured = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .storage(CoredeuxStorageDefinition.builder().build())
                .modules(List.of())
                .build();

        DefaultCoredeuxEntityDefinitionResolver resolver = new DefaultCoredeuxEntityDefinitionResolver(
                new SimpleRegistry(List.of(configured)),
                new CoredeuxProperties(java.util.Map.of("data-access-service", "globalDataAccess")));

        CoredeuxEntityDefinition definition = resolver.resolve(SampleEntity.class);

        assertEquals("sample", definition.getName());
        assertNull(definition.getIdentifier());
        assertEquals("globalDataAccess", definition.getStorage().getDataAccessService());
    }

    @Test
    void shouldUseGlobalIdentifierWhenDefinitionAndStorageOmitOne() {
        CoredeuxEntityDefinition configured = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("explicitDataAccess").build())
                .modules(List.of())
                .build();

        DefaultCoredeuxEntityDefinitionResolver resolver = new DefaultCoredeuxEntityDefinitionResolver(
                new SimpleRegistry(List.of(configured)),
                new CoredeuxProperties(java.util.Map.of("data-access-service", "globalDataAccess", "identifier",
                        "globalIdentifier")));

        CoredeuxEntityDefinition definition = resolver.resolve(SampleEntity.class);

        assertEquals("globalIdentifier", definition.getIdentifier());
        assertEquals("explicitDataAccess", definition.getStorage().getDataAccessService());
    }

    private static final class SimpleRegistry implements EntityDefinitionRegistry {

        private final List<CoredeuxEntityDefinition> definitions;

        private SimpleRegistry(List<CoredeuxEntityDefinition> definitions) {
            this.definitions = definitions;
        }

        @Override
        public Optional<CoredeuxEntityDefinition> findByFullClassName(String fullClassName) {
            return definitions.stream().filter(def -> fullClassName.equals(def.getFullClassName())).findFirst();
        }

        @Override
        public Collection<CoredeuxEntityDefinition> getAll() {
            return definitions;
        }
    }

    private static final class SampleEntity {
        private String id;
    }
}
