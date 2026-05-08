package com.coredeux.core.resolver.context;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.coredeux.core.context.RequestContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Default servlet-backed request context resolver.
 */
@Service
public class DefaultCoredeuxRequestContextResolver implements CoredeuxRequestContextResolver {

    private static final List<String> REQUEST_ID_KEYS = List.of("X-Request-Id", "requestId");
    private static final List<String> CORRELATION_ID_KEYS = List.of("X-Correlation-Id", "correlationId");
    private static final List<String> USER_ID_KEYS = List.of("X-User-Id", "userId");
    private static final List<String> TENANT_ID_KEYS = List.of("X-Tenant-Id", "tenantId", "X-Site-Id", "siteId");

    @Override
    public RequestContext resolve() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        if (request == null) {
            return null;
        }

        Locale locale = request.getLocale();
        return RequestContext.builder()
                .requestId(resolveValue(request, REQUEST_ID_KEYS))
                .correlationId(resolveValue(request, CORRELATION_ID_KEYS))
                .userId(resolveValue(request, USER_ID_KEYS))
                .tenantId(resolveValue(request, TENANT_ID_KEYS))
                .locale(locale)
                .build();
    }

    private String resolveValue(HttpServletRequest request, List<String> keys) {
        for (String key : keys) {
            String headerValue = trimToNull(request.getHeader(key));
            if (headerValue != null) {
                return headerValue;
            }

            String parameterValue = trimToNull(request.getParameter(key));
            if (parameterValue != null) {
                return parameterValue;
            }

            Object attributeValue = request.getAttribute(key);
            if (attributeValue instanceof String attributeString) {
                String trimmedAttribute = trimToNull(attributeString);
                if (trimmedAttribute != null) {
                    return trimmedAttribute;
                }
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }
}
