package com.coredeux.spring.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.coredeux.core.context.RequestContext;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;

class CoredeuxWebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxWebAutoConfiguration.class,
                    CoredeuxRequestContextAutoConfiguration.class));

    @Test
    void resolvesRequestContextFromServletRequest() {
        contextRunner.run(context -> {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Request-Id", "req-1");
            request.addHeader("X-Correlation-Id", "corr-1");
            request.addHeader("X-User-Id", "user-1");
            request.addHeader("X-Tenant-Id", "tenant-1");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            try {
                CoredeuxRequestContextResolver resolver = context.getBean(CoredeuxRequestContextResolver.class);
                RequestContext requestContext = resolver.resolve();

                assertThat(requestContext.getRequestId()).isEqualTo("req-1");
                assertThat(requestContext.getCorrelationId()).isEqualTo("corr-1");
                assertThat(requestContext.getUserId()).isEqualTo("user-1");
                assertThat(requestContext.getTenantId()).isEqualTo("tenant-1");
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        });
    }
}
