package com.coredeux.impex.handler;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.exception.CoredeuxImportException;

class ImportValueHandlerResolverTest {

    private final CoredeuxImportValueHandler defaultHandler = context -> context.getEffectiveValue();
    private final CoredeuxImportValueHandler namedHandler = context -> "named";

    @Test
    void shouldResolveDefaultHandlerForBlankNames() {
        ImportValueHandlerResolver resolver = new ImportValueHandlerResolver(Map.of(
                ImportValueHandlerResolver.DEFAULT_HANDLER, defaultHandler));

        assertSame(defaultHandler, resolver.resolve(null));
        assertSame(defaultHandler, resolver.resolve(" "));
    }

    @Test
    void shouldTrimAndResolveNamedHandler() {
        ImportValueHandlerResolver resolver = new ImportValueHandlerResolver(Map.of("customHandler", namedHandler));

        assertSame(namedHandler, resolver.resolve(" customHandler "));
    }

    @Test
    void shouldRejectMissingHandler() {
        ImportValueHandlerResolver resolver = new ImportValueHandlerResolver(null);

        CoredeuxImportException exception = assertThrows(CoredeuxImportException.class,
                () -> resolver.resolve("missingHandler"));

        assertTrue(exception.getMessage().contains("Import value handler not found: missingHandler"));
    }
}
