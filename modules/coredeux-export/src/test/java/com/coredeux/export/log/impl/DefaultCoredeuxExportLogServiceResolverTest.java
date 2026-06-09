package com.coredeux.export.log.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.model.ExportLogEntry;

class DefaultCoredeuxExportLogServiceResolverTest {

    @Test
    void resolvesExplicitDefaultAndSingleServiceFallback() {
        CoredeuxExportLogService only = new NoOpLogService();
        DefaultCoredeuxExportLogServiceResolver resolver =
                new DefaultCoredeuxExportLogServiceResolver(Map.of("only", only), "only");

        assertSame(only, resolver.resolve(null));
        assertSame(only, resolver.resolve(" "));
        assertSame(only, resolver.resolve("only"));
    }

    @Test
    void throwsWhenNoMatchingServiceExists() {
        DefaultCoredeuxExportLogServiceResolver resolver =
                new DefaultCoredeuxExportLogServiceResolver(Map.of(
                        "one", new NoOpLogService(),
                        "two", new NoOpLogService()));

        assertThrows(CoredeuxExportException.class, () -> resolver.resolve("missing"));
    }

    private static final class NoOpLogService implements CoredeuxExportLogService {

        @Override
        public void info(String uid, String message, Map<String, Object> metadata) {
        }

        @Override
        public void warn(String uid, String message, Map<String, Object> metadata) {
        }

        @Override
        public void error(String uid, String message, Throwable error, Map<String, Object> metadata) {
        }

        @Override
        public List<ExportLogEntry> findByUid(String uid) {
            return List.of();
        }
    }
}
