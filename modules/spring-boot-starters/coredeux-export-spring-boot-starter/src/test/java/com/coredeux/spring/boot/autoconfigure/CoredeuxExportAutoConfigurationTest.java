package com.coredeux.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.handler.service.CoredeuxValueHandlerService;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.registry.InMemoryEntityDefinitionRegistry;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.export.log.impl.DefaultCoredeuxExportLogServiceResolver;
import com.coredeux.export.queue.impl.FileCoredeuxExportQueueService;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.service.CoredeuxExportService;
import com.coredeux.export.storage.impl.DefaultCoredeuxExportStorageServiceResolver;
import com.coredeux.export.storage.impl.DefaultCoredeuxFileSystemExportStorageService;
import com.coredeux.export.worker.DefaultCoredeuxExportWorker;
import com.coredeux.spring.boot.autoconfigure.CoredeuxExportWorkerScheduler;
import com.coredeux.export.writer.ExcelExportWriter;
import com.coredeux.export.writer.TextExportWriter;

class CoredeuxExportAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxAutoConfiguration.class,
                    CoredeuxRequestContextAutoConfiguration.class, CoredeuxExportAutoConfiguration.class))
            .withBean(CoredeuxService.class, this::sampleCoredeuxService)
            .withBean(EntityDefinitionRegistry.class, this::sampleEntityDefinitionRegistry)
            .withBean(CoredeuxReflectionHelperService.class, DefaultCoredeuxReflectionHelperService::new);

    @Test
    void registersExportBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(CoredeuxValueHandlerService.class)
                .hasSingleBean(DefaultCoredeuxExportLogServiceResolver.class)
                .hasSingleBean(DefaultCoredeuxExportStorageServiceResolver.class)
                .hasSingleBean(FileCoredeuxExportQueueService.class)
                .hasSingleBean(TextExportWriter.class)
                .hasSingleBean(ExcelExportWriter.class)
                .hasSingleBean(DefaultCoredeuxExportWorker.class)
                .hasSingleBean(CoredeuxExportWorkerScheduler.class)
                .hasSingleBean(CoredeuxExportService.class));
    }

    @Test
    void readsExportDefaultsFromCoredeuxYaml() {
        contextRunner.run(context -> {
            DefaultCoredeuxExportWorker worker = context.getBean(DefaultCoredeuxExportWorker.class);
            assertFalse((Boolean) ReflectionTestUtils.getField(worker, "enabled"));
            assertEquals(3, ReflectionTestUtils.getField(worker, "maxParallel"));
            assertEquals(ExportFormat.TEXT, context.getBean(CoredeuxExportProperties.class).defaultFormat());

            FileCoredeuxExportQueueService queueService = context.getBean(FileCoredeuxExportQueueService.class);
            assertEquals("target/coredeux-export-test/queue/export-queue.json",
                    ReflectionTestUtils.getField(queueService, "queueFile").toString().replace('\\', '/'));

            DefaultCoredeuxFileSystemExportStorageService storageService = (DefaultCoredeuxFileSystemExportStorageService) context
                    .getBean(DefaultCoredeuxExportStorageServiceResolver.DEFAULT_STORAGE_SERVICE);
            assertEquals("target/coredeux-export-test/storage",
                    ReflectionTestUtils.getField(storageService, "baseDirectory").toString().replace('\\', '/'));
        });
    }

    @SuppressWarnings("unused")
    private CoredeuxService sampleCoredeuxService() {
        return new CoredeuxService() {
            @Override
            public <T> T load(String id, Class<T> type) {
                return null;
            }

            @Override
            public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                    int currentPage) {
                return SearchResult.<T>builder().results(List.of()).build();
            }

            @Override
            public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize,
                    int currentPage) {
                return SearchResult.<T>builder().results(List.of()).build();
            }

            @Override
            public <T> String save(T entity) {
                return null;
            }

            @Override
            public <T> void update(T entity) {
            }

            @Override
            public <T> void remove(String id, Class<T> type) {
            }

            @Override
            public <T> void remove(T entity) {
            }

            @Override
            public <T> void refresh(T entity) {
            }

            @Override
            public Set<String> supportedComparators(Class<?> type) {
                return Set.of();
            }
        };
    }

    @SuppressWarnings("unused")
    private EntityDefinitionRegistry sampleEntityDefinitionRegistry() {
        return new InMemoryEntityDefinitionRegistry(List.of(CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .identifier("id")
                .build()));
    }

    static class SampleEntity {
        private String id;

        String getId() {
            return id;
        }
    }
}
