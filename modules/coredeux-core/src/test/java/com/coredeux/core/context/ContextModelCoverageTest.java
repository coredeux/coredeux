package com.coredeux.core.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Instant;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class ContextModelCoverageTest {

    @Test
    void shouldExerciseRequestContextBuilderEqualityAndAccessors() {
        RequestContext requestContext = RequestContext.builder()
                .requestId("req-1")
                .correlationId("corr-1")
                .userId("user-1")
                .tenantId("tenant-1")
                .locale(Locale.ENGLISH)
                .build();
        RequestContext sameRequestContext = RequestContext.builder()
                .requestId("req-1")
                .correlationId("corr-1")
                .userId("user-1")
                .tenantId("tenant-1")
                .locale(Locale.ENGLISH)
                .build();
        RequestContext differentRequestContext = RequestContext.builder()
                .requestId("req-2")
                .correlationId("corr-2")
                .userId("user-2")
                .tenantId("tenant-2")
                .locale(Locale.FRENCH)
                .build();

        assertEquals("req-1", requestContext.getRequestId());
        assertEquals("corr-1", requestContext.getCorrelationId());
        assertEquals("user-1", requestContext.getUserId());
        assertEquals("tenant-1", requestContext.getTenantId());
        assertEquals(Locale.ENGLISH, requestContext.getLocale());
        assertEquals(requestContext, sameRequestContext);
        assertEquals(requestContext.hashCode(), sameRequestContext.hashCode());
        assertNotEquals(requestContext, differentRequestContext);
        assertNotNull(requestContext.toString());
    }

    @Test
    void shouldExerciseEntityLifecycleContextBuilderEqualityAndAccessors() {
        EntityLifecycleContext<String> lifecycleContext = EntityLifecycleContext.<String>builder()
                .operation("MODIFY")
                .identifier("123")
                .oldValue("old")
                .newValue("new")
                .build();
        EntityLifecycleContext<String> sameLifecycleContext = EntityLifecycleContext.<String>builder()
                .operation("MODIFY")
                .identifier("123")
                .oldValue("old")
                .newValue("new")
                .build();
        EntityLifecycleContext<String> differentLifecycleContext = EntityLifecycleContext.<String>builder()
                .operation("DELETE")
                .identifier("999")
                .oldValue("gone")
                .newValue(null)
                .build();

        assertEquals("MODIFY", lifecycleContext.getOperation());
        assertEquals("123", lifecycleContext.getIdentifier());
        assertEquals("old", lifecycleContext.getOldValue());
        assertEquals("new", lifecycleContext.getNewValue());
        assertEquals(lifecycleContext, sameLifecycleContext);
        assertEquals(lifecycleContext.hashCode(), sameLifecycleContext.hashCode());
        assertNotEquals(lifecycleContext, differentLifecycleContext);
        assertNotNull(lifecycleContext.toString());
    }

    @Test
    void shouldExerciseOperationContextEmptyBuilderAndLifecycleReplacement() {
        OperationContext empty = OperationContext.empty();
        assertNull(empty.getInvokedAt());
        assertNull(empty.getRequestContext());
        assertNull(empty.getLifecycleContext());

        RequestContext requestContext = RequestContext.builder()
                .requestId("req-1")
                .correlationId("corr-1")
                .userId("user-1")
                .tenantId("tenant-1")
                .locale(Locale.ENGLISH)
                .build();
        Instant invokedAt = Instant.now();
        EntityLifecycleContext<String> lifecycleContext = EntityLifecycleContext.<String>builder()
                .operation("CREATE")
                .identifier("1")
                .oldValue(null)
                .newValue("value")
                .build();
        EntityLifecycleContext<String> replacementLifecycleContext = EntityLifecycleContext.<String>builder()
                .operation("UPSERT")
                .identifier("1")
                .oldValue("old")
                .newValue("value")
                .build();

        OperationContext operationContext = OperationContext.builder()
                .invokedAt(invokedAt)
                .requestContext(requestContext)
                .lifecycleContext(lifecycleContext)
                .build();
        OperationContext sameOperationContext = OperationContext.builder()
                .invokedAt(invokedAt)
                .requestContext(requestContext)
                .lifecycleContext(lifecycleContext)
                .build();
        OperationContext updatedOperationContext = operationContext.withLifecycleContext(replacementLifecycleContext);

        assertSame(invokedAt, operationContext.getInvokedAt());
        assertSame(requestContext, operationContext.getRequestContext());
        assertSame(lifecycleContext, operationContext.getLifecycleContext());
        assertEquals(operationContext, sameOperationContext);
        assertEquals(operationContext.hashCode(), sameOperationContext.hashCode());
        assertNotEquals(operationContext, updatedOperationContext);
        assertSame(invokedAt, updatedOperationContext.getInvokedAt());
        assertSame(requestContext, updatedOperationContext.getRequestContext());
        assertSame(replacementLifecycleContext, updatedOperationContext.getLifecycleContext());
        assertNotNull(operationContext.toString());
        assertNotNull(updatedOperationContext.toString());
    }
}
