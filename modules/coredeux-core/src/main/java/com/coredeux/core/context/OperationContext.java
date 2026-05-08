package com.coredeux.core.context;

import java.io.Serializable;
import java.time.Instant;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Carries execution metadata used by service and strategy layers for a single
 * operation.
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public final class OperationContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final OperationContext EMPTY = OperationContext.builder().build();

    private final Instant invokedAt;
    private final RequestContext requestContext;
    private final EntityLifecycleContext<?> lifecycleContext;

    /**
     * Returns an empty operation context for callers that do not need to pass
     * request metadata explicitly.
     *
     * @return an empty operation context
     */
    public static OperationContext empty() {
        return EMPTY;
    }

    public OperationContext withLifecycleContext(EntityLifecycleContext<?> lifecycleContext) {
        return OperationContext.builder()
                .invokedAt(invokedAt)
                .requestContext(requestContext)
                .lifecycleContext(lifecycleContext)
                .build();
    }
}
