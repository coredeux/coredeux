package com.coredeux.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.coredeux.core.context.RequestContext;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;

class CoredeuxRequestContextAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxRequestContextAutoConfiguration.class));

    @Test
    void providesNullResolverFallback() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CoredeuxRequestContextResolver.class);
            assertThat(context.getBean(CoredeuxRequestContextResolver.class).resolve()).isNull();
        });
    }

    @Test
    void backsOffWhenCustomResolverExists() {
        CoredeuxRequestContextResolver resolver = () -> RequestContext.builder().requestId("custom").build();

        contextRunner.withBean(CoredeuxRequestContextResolver.class, () -> resolver)
                .run(context -> {
                    assertThat(context).hasSingleBean(CoredeuxRequestContextResolver.class);
                    assertThat(context.getBean(CoredeuxRequestContextResolver.class)).isSameAs(resolver);
                    assertThat(context.getBean(CoredeuxRequestContextResolver.class).resolve().getRequestId())
                            .isEqualTo("custom");
                });
    }
}
