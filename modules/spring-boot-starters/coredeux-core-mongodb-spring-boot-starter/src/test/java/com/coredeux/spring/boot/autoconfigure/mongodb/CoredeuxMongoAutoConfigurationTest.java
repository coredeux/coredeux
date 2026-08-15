package com.coredeux.spring.boot.autoconfigure.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.coredeux.core.mongodb.service.impl.DefaultCoredeuxMongoDataAccessService;

class CoredeuxMongoAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxMongoAutoConfiguration.class))
            .withBean(MongoTemplate.class, () -> mock(MongoTemplate.class));

    @Test
    void registersMongoService() {
        contextRunner.withPropertyValues("coredeux.mongodb.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(DefaultCoredeuxMongoDataAccessService.class));
    }

    @Test
    void doesNotRegisterMongoServiceWhenNotEnabled() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(DefaultCoredeuxMongoDataAccessService.class));
    }

    @Test
    void backsOffWhenMongoServiceAlreadyExists() {
        DefaultCoredeuxMongoDataAccessService customService =
                new DefaultCoredeuxMongoDataAccessService(mock(MongoTemplate.class));

        contextRunner.withPropertyValues("coredeux.mongodb.enabled=true")
                .withBean("defaultCoredeuxMongoDataAccessService",
                        DefaultCoredeuxMongoDataAccessService.class, () -> customService)
                .run(context -> assertThat(context.getBean(DefaultCoredeuxMongoDataAccessService.class))
                        .isSameAs(customService));
    }
}
