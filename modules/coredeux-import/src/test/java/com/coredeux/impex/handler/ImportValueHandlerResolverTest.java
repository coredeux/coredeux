package com.coredeux.impex.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxValueHandlerException;
import com.coredeux.core.handler.service.impl.DefaultCoredeuxValueHandlerService;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;

class ImportValueHandlerResolverTest {

    @Test
    void shouldTrimAndResolveNamedHandler() {
        DefaultCoredeuxValueHandlerService service = new DefaultCoredeuxValueHandlerService(
                InMemoryCoredeuxComponentRegistry.builder()
                        .component("customHandler", (CoredeuxImportValueHandler) context -> "named")
                        .build());

        assertEquals("named", service.invoke(" customHandler ", context("demo")));
    }

    @Test
    void shouldRejectMissingHandler() {
        DefaultCoredeuxValueHandlerService service = new DefaultCoredeuxValueHandlerService(
                InMemoryCoredeuxComponentRegistry.builder().build());

        CoredeuxValueHandlerException exception = assertThrows(CoredeuxValueHandlerException.class,
                () -> service.invoke("missingHandler", context("demo")));

        assertTrue(exception.getMessage().contains("Unable to resolve value handler: missingHandler"));
    }

    @Test
    void shouldRejectBlankHandlerNames() {
        DefaultCoredeuxValueHandlerService service = new DefaultCoredeuxValueHandlerService(
                InMemoryCoredeuxComponentRegistry.builder().build());

        CoredeuxValueHandlerException exception = assertThrows(CoredeuxValueHandlerException.class,
                () -> service.invoke(" ", context("demo")));

        assertEquals("Value handler name is required", exception.getMessage());
    }

    private ImportValueContext context(String effectiveValue) {
        return ImportValueContext.builder()
                .effectiveValue(effectiveValue)
                .build();
    }
}
