package com.coredeux.core.context;

import java.io.Serializable;
import java.util.Locale;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Request-scoped metadata associated with the current caller.
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public final class RequestContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final String correlationId;
    private final String userId;
    private final String tenantId;
    private final Locale locale;
}
