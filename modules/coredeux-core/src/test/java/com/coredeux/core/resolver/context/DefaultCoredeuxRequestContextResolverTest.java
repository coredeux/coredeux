package com.coredeux.core.resolver.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.coredeux.core.context.RequestContext;

class DefaultCoredeuxRequestContextResolverTest {

    private final DefaultCoredeuxRequestContextResolver resolver = new DefaultCoredeuxRequestContextResolver();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldResolveRequestContextFromServletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-1");
        request.addHeader("X-Correlation-Id", "corr-1");
        request.addHeader("X-User-Id", "user-1");
        request.addHeader("X-Site-Id", "site-1");
        request.setPreferredLocales(java.util.List.of(Locale.ENGLISH));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestContext context = resolver.resolve();

        assertNotNull(context);
        assertEquals("req-1", context.getRequestId());
        assertEquals("corr-1", context.getCorrelationId());
        assertEquals("user-1", context.getUserId());
        assertEquals("site-1", context.getTenantId());
        assertEquals(Locale.ENGLISH, context.getLocale());
    }

    @Test
    void shouldResolveFromParametersAndAttributesWhenHeadersAreMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("requestId", "req-2");
        request.setParameter("correlationId", "corr-2");
        request.setAttribute("userId", "user-2");
        request.setAttribute("tenantId", "tenant-2");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestContext context = resolver.resolve();

        assertNotNull(context);
        assertEquals("req-2", context.getRequestId());
        assertEquals("corr-2", context.getCorrelationId());
        assertEquals("user-2", context.getUserId());
        assertEquals("tenant-2", context.getTenantId());
    }

    @Test
    void shouldReturnNullWhenNoServletRequestIsAvailable() {
        assertNull(resolver.resolve());
    }
}
