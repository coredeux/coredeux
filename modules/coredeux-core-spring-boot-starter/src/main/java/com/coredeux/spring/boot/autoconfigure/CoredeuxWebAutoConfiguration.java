package com.coredeux.spring.boot.autoconfigure;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.coredeux.core.context.RequestContext;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;

@AutoConfiguration(after = CoredeuxAutoConfiguration.class)
@ConditionalOnClass(name = {
        "jakarta.servlet.http.HttpServletRequest",
        "org.springframework.web.context.request.RequestContextHolder"
})
public class CoredeuxWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CoredeuxRequestContextResolver coredeuxServletRequestContextResolver() {
        return new ServletCoredeuxRequestContextResolver();
    }

    static final class ServletCoredeuxRequestContextResolver implements CoredeuxRequestContextResolver {

        @Override
        public RequestContext resolve() {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();
            return RequestContext.builder()
                    .requestId(firstValue(request, "X-Request-Id", "requestId"))
                    .correlationId(firstValue(request, "X-Correlation-Id", "correlationId"))
                    .userId(firstValue(request, "X-User-Id", "userId"))
                    .tenantId(firstValue(request, "X-Tenant-Id", "tenantId", "X-Site-Id", "siteId"))
                    .locale(resolveLocale(request))
                    .build();
        }

        private static String firstValue(HttpServletRequest request, String... names) {
            for (String name : names) {
                String value = request.getHeader(name);
                if (value == null || value.isBlank()) {
                    value = request.getParameter(name);
                }
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }

        private static Locale resolveLocale(HttpServletRequest request) {
            Locale locale = request.getLocale();
            return locale == null ? Locale.getDefault() : locale;
        }
    }
}
