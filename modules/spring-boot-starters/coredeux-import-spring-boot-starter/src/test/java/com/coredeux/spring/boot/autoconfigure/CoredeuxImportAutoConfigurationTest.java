package com.coredeux.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.coredeux.core.handler.service.CoredeuxValueHandlerService;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.impex.parser.excel.CoredeuxExcelImportParser;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;
import com.coredeux.impex.service.CoredeuxImportService;
import com.coredeux.impex.service.impl.ImportEntityTargetService;

class CoredeuxImportAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxAutoConfiguration.class,
                    CoredeuxRequestContextAutoConfiguration.class,
                    CoredeuxImportAutoConfiguration.class))
            .withBean(CoredeuxService.class, this::sampleCoredeuxService)
            .withBean(EntityDefinitionRegistry.class, this::sampleEntityDefinitionRegistry)
            .withBean(CoredeuxReflectionHelperService.class, DefaultCoredeuxReflectionHelperService::new);

    @Test
    void registersImportBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(CoredeuxImportProperties.class)
                .hasSingleBean(CoredeuxTextImportParser.class)
                .hasSingleBean(CoredeuxExcelImportParser.class)
                .hasSingleBean(CoredeuxValueHandlerService.class)
                .hasSingleBean(ImportEntityTargetService.class)
                .hasSingleBean(CoredeuxImportService.class));
    }

    @Test
    void readsDefaultParserFromCoredeuxYaml() {
        contextRunner.run(context -> assertThat(context.getBean(CoredeuxImportProperties.class).defaultParser())
                .isEqualTo("excel"));
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
            public <T> void remove(T entity) {
            }

            @Override
            public <T> void remove(String id, Class<T> type) {
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
        return new EntityDefinitionRegistry() {
            @Override
            public java.util.Optional<com.coredeux.core.definition.CoredeuxEntityDefinition> findByFullClassName(
                    String fullClassName) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Collection<com.coredeux.core.definition.CoredeuxEntityDefinition> getAll() {
                return java.util.List.of();
            }
        };
    }
}
