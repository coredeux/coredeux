package com.coredeux.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxStrategyException;

class InMemoryCoredeuxComponentRegistryTest {

    @Test
    void shouldResolveRegisteredComponentByNameAndType() {
        SampleContract component = new SampleComponent();
        InMemoryCoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder()
                .component("sample", component)
                .build();

        assertSame(component, registry.getComponent("sample", SampleContract.class));
    }

    @Test
    void shouldResolveComponentsFromConstructorMap() {
        SampleContract component = new SampleComponent();
        InMemoryCoredeuxComponentRegistry registry = new InMemoryCoredeuxComponentRegistry(Map.of("sample", component));

        assertSame(component, registry.getComponent("sample", SampleContract.class));
    }

    @Test
    void shouldRejectMissingComponent() {
        InMemoryCoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder().build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> registry.getComponent("missing", SampleContract.class));

        assertEquals("Unable to resolve Coredeux component: missing", exception.getMessage());
    }

    @Test
    void shouldRejectComponentWithWrongType() {
        InMemoryCoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder()
                .component("sample", "not-a-component")
                .build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> registry.getComponent("sample", SampleContract.class));

        assertEquals("Coredeux component 'sample' does not implement "
                + SampleContract.class.getName() + ": java.lang.String", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidBuilderEntries() {
        InMemoryCoredeuxComponentRegistry.Builder builder = InMemoryCoredeuxComponentRegistry.builder();

        assertThrows(CoredeuxStrategyException.class, () -> builder.component(" ", new SampleComponent()));
        assertThrows(CoredeuxStrategyException.class, () -> builder.component("sample", null));
    }

    interface SampleContract {
    }

    static class SampleComponent implements SampleContract {
    }
}
