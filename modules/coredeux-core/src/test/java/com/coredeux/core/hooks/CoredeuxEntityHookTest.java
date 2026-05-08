package com.coredeux.core.hooks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;

class CoredeuxEntityHookTest {

    @Test
    void shouldAllowDefaultLifecycleMethods() {
        CoredeuxEntityHook<Object> hook = new CoredeuxEntityHook<>() {
        };
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("com.example.Customer").build();
        OperationContext context = OperationContext.empty();
        Object entity = new Object();

        assertDoesNotThrow(() -> hook.onLoad(entity, definition, context));
        assertDoesNotThrow(() -> hook.beforeSave(entity, definition, context));
        assertDoesNotThrow(() -> hook.afterSave(entity, definition, context));
        assertDoesNotThrow(() -> hook.beforeUpdate(entity, definition, context));
        assertDoesNotThrow(() -> hook.afterUpdate(entity, definition, context));
        assertDoesNotThrow(() -> hook.beforeDelete(entity, definition, context));
        assertDoesNotThrow(() -> hook.beforeRefresh(entity, definition, context));
        assertDoesNotThrow(() -> hook.afterRefresh(entity, definition, context));
    }
}
