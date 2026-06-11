package com.coredeux.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.coredeux.core.handler.service.CoredeuxValueHandlerService;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.service.CoredeuxService;

class CoredeuxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxAutoConfiguration.class,
                    CoredeuxRequestContextAutoConfiguration.class));

    @Test
    void registersCoreBeansIncludingTheValueHandlerService() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(CoredeuxComponentRegistry.class)
                .hasSingleBean(CoredeuxValueHandlerService.class)
                .hasSingleBean(CoredeuxService.class));
    }
}
