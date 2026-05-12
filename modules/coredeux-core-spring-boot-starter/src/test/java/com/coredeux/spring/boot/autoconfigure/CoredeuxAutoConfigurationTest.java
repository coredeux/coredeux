package com.coredeux.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;
import com.coredeux.core.service.CoredeuxModuleService;
import com.coredeux.core.service.CoredeuxService;

class CoredeuxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxAutoConfiguration.class,
                    CoredeuxRequestContextAutoConfiguration.class))
            .withBean("sampleDataAccess", SampleDataAccess.class);

    @Test
    void registersCoreBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(EntityDefinitionRegistry.class)
                .hasSingleBean(CoredeuxComponentRegistry.class)
                .hasSingleBean(CoredeuxModuleService.class)
                .hasSingleBean(CoredeuxService.class));
    }

    @Test
    void resolvesApplicationBeansThroughComponentRegistry() {
        contextRunner.run(context -> {
            CoredeuxComponentRegistry registry = context.getBean(CoredeuxComponentRegistry.class);

            assertThat(registry.getComponent("sampleDataAccess", CoredeuxDataAccessService.class))
                    .isSameAs(context.getBean("sampleDataAccess"));
        });
    }

    static class SampleEntity {

        private String id;

        String getId() {
            return id;
        }
    }

    static class SampleDataAccess implements CoredeuxDataAccessService {

        @Override
        public <T> T load(String id, Class<T> type) {
            return null;
        }

        @Override
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type,
                int pageSize, int currentPage) {
            return SearchResult.<T>builder().results(List.of()).build();
        }

        @Override
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize,
                int currentPage) {
            return SearchResult.<T>builder().results(List.of()).build();
        }

        @Override
        public Set<String> supportedComparators(Class<?> type) {
            return Set.of();
        }

        @Override
        public <T> String save(T entity) {
            return entity instanceof SampleEntity sample ? sample.getId() : null;
        }

        @Override
        public <T> void update(T entity) {
        }

        @Override
        public <T> void remove(T entity) {
        }

        @Override
        public <T> void refresh(T entity) {
        }
    }
}
