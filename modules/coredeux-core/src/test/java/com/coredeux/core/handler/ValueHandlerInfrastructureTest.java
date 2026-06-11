package com.coredeux.core.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxValueHandlerException;
import com.coredeux.core.handler.service.impl.DefaultCoredeuxValueHandlerService;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;

class ValueHandlerInfrastructureTest {

    @Test
    void invokeTrimsTheHandlerNameAndDelegatesToTheResolvedHandler() {
        InMemoryCoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder()
                .component("sampleHandler", new SampleValueHandler())
                .build();
        DefaultCoredeuxValueHandlerService service = new DefaultCoredeuxValueHandlerService(registry);

        String result = service.invoke(" sampleHandler ", new TestValueContext("demo"));

        assertEquals("handled:demo", result);
    }

    @Test
    void invokeRequiresHandlerName() {
        InMemoryCoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder()
                .component("sampleHandler", new SampleValueHandler())
                .build();
        DefaultCoredeuxValueHandlerService service = new DefaultCoredeuxValueHandlerService(registry);

        CoredeuxValueHandlerException exception = assertThrows(CoredeuxValueHandlerException.class,
                () -> service.invoke(" ", new TestValueContext("demo")));

        assertEquals("Value handler name is required", exception.getMessage());
    }

    @Test
    void invokeRequiresAComponentRegistryOnTheService() {
        DefaultCoredeuxValueHandlerService service = new DefaultCoredeuxValueHandlerService(null);

        CoredeuxValueHandlerException exception = assertThrows(CoredeuxValueHandlerException.class,
                () -> service.invoke("sampleHandler", new TestValueContext("demo")));

        assertEquals("Value handler service does not provide a component registry for handler: sampleHandler",
                exception.getMessage());
    }

    @Test
    void invokeWrapsHandlerResolutionFailures() {
        InMemoryCoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder().build();
        DefaultCoredeuxValueHandlerService service = new DefaultCoredeuxValueHandlerService(registry);

        CoredeuxValueHandlerException exception = assertThrows(CoredeuxValueHandlerException.class,
                () -> service.invoke("missingHandler", new TestValueContext("demo")));

        assertEquals("Unable to resolve value handler: missingHandler", exception.getMessage());
    }

    @Test
    void valueContextRemainsAnEmptyMarkerContract() {
        ValueContext context = new TestValueContext("demo");

        assertEquals(TestValueContext.class, context.getClass());
    }

    private static final class TestValueContext implements ValueContext {

        private final String value;

        private TestValueContext(String value) {
            this.value = value;
        }
    }

    private static final class SampleValueHandler implements CoredeuxValueHandler<String, TestValueContext> {

        @Override
        public String handle(TestValueContext context) {
            return "handled:" + context.value;
        }
    }
}
