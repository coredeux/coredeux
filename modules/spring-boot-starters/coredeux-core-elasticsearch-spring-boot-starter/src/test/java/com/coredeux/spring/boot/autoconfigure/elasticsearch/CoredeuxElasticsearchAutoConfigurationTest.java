package com.coredeux.spring.boot.autoconfigure.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import com.coredeux.core.elasticsearch.service.impl.DefaultCoredeuxElasticsearchDataAccessService;

class CoredeuxElasticsearchAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxElasticsearchAutoConfiguration.class))
            .withBean(ElasticsearchOperations.class, () -> mock(ElasticsearchOperations.class));

    @Test
    void registersElasticsearchService() {
        contextRunner.withPropertyValues("coredeux.elasticsearch.enabled=true",
                "coredeux.elasticsearch.default-index-prefix=demo")
                .run(context -> assertThat(context).hasSingleBean(DefaultCoredeuxElasticsearchDataAccessService.class));
    }

    @Test
    void doesNotRegisterElasticsearchServiceWhenNotEnabled() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(DefaultCoredeuxElasticsearchDataAccessService.class));
    }

    @Test
    void backsOffWhenElasticsearchServiceAlreadyExists() {
        DefaultCoredeuxElasticsearchDataAccessService customService =
                new DefaultCoredeuxElasticsearchDataAccessService(mock(ElasticsearchOperations.class), "custom");

        contextRunner.withPropertyValues("coredeux.elasticsearch.enabled=true")
                .withBean("defaultCoredeuxElasticsearchDataAccessService",
                        DefaultCoredeuxElasticsearchDataAccessService.class, () -> customService)
                .run(context -> assertThat(context.getBean(DefaultCoredeuxElasticsearchDataAccessService.class))
                        .isSameAs(customService));
    }
}
