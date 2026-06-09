package com.coredeux.export.storage.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportStorageArtifact;
import com.coredeux.export.model.ExportStorageRequest;
import com.coredeux.export.storage.CoredeuxExportStorageService;

class DefaultCoredeuxExportStorageServiceResolverTest {

    @Test
    void resolvesExplicitDefaultAndSingleServiceFallback() {
        CoredeuxExportStorageService only = request -> artifact();
        DefaultCoredeuxExportStorageServiceResolver resolver =
                new DefaultCoredeuxExportStorageServiceResolver(Map.of("only", only), "only");

        assertSame(only, resolver.resolve(null));
        assertSame(only, resolver.resolve(" "));
        assertSame(only, resolver.resolve("only"));
    }

    @Test
    void throwsWhenNoMatchingServiceExists() {
        DefaultCoredeuxExportStorageServiceResolver resolver =
                new DefaultCoredeuxExportStorageServiceResolver(Map.of(
                        "one", request -> artifact(),
                        "two", request -> artifact()));

        assertThrows(CoredeuxExportException.class, () -> resolver.resolve("missing"));
    }

    private ExportStorageArtifact artifact() {
        return ExportStorageArtifact.builder().fileName("demo").build();
    }
}
