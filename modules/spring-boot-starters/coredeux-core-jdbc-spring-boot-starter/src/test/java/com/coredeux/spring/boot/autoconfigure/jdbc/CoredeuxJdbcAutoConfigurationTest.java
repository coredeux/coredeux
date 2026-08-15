package com.coredeux.spring.boot.autoconfigure.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.coredeux.core.jdbc.service.impl.DefaultCoredeuxJdbcDataAccessService;

class CoredeuxJdbcAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxJdbcAutoConfiguration.class))
            .withBean(NamedParameterJdbcTemplate.class, () -> mock(NamedParameterJdbcTemplate.class));

    @Test
    void registersJdbcServiceAndHonorsDefaultSchemaProperty() {
        contextRunner.withPropertyValues("coredeux.jdbc.enabled=true",
                "coredeux.jdbc.default-schema=demo")
                .run(context -> {
                    assertThat(context).hasSingleBean(DefaultCoredeuxJdbcDataAccessService.class);
                    assertThat(context.getBean(DefaultCoredeuxJdbcDataAccessService.class)).isNotNull();
                });
    }

    @Test
    void doesNotRegisterJdbcServiceWhenNotEnabled() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(DefaultCoredeuxJdbcDataAccessService.class));
    }

    @Test
    void backsOffWhenJdbcServiceAlreadyExists() {
        DefaultCoredeuxJdbcDataAccessService customService =
                new DefaultCoredeuxJdbcDataAccessService(mock(NamedParameterJdbcTemplate.class), "custom");

        contextRunner.withPropertyValues("coredeux.jdbc.enabled=true")
                .withBean("defaultCoredeuxJdbcDataAccessService",
                        DefaultCoredeuxJdbcDataAccessService.class, () -> customService)
                .run(context -> assertThat(context.getBean(DefaultCoredeuxJdbcDataAccessService.class))
                        .isSameAs(customService));
    }
}
