package com.coredeux.core.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import com.coredeux.core.testsupport.TestComponentRegistry;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.core.strategy.CoredeuxHookPhases;

class HooksModuleHandlerTest {

    @Test
    void shouldInvokeSupportedHookPhases() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        RecordingHook hook = new RecordingHook();
        applicationContext.registerSingleton("lifecycleHook", hook);
        HooksModuleHandler handler = new HooksModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                .handlers(List.of("lifecycleHook")).build();
        SampleEntity entity = new SampleEntity();
        OperationContext context = OperationContext.empty();

        handler.execute(entity, definition, module, CoredeuxHookPhases.LOAD, context);
        handler.execute(entity, definition, module, CoredeuxHookPhases.BEFORE_SAVE, context);
        handler.execute(entity, definition, module, CoredeuxHookPhases.AFTER_SAVE, context);
        handler.execute(entity, definition, module, CoredeuxHookPhases.BEFORE_UPDATE, context);
        handler.execute(entity, definition, module, CoredeuxHookPhases.AFTER_UPDATE, context);
        handler.execute(entity, definition, module, CoredeuxHookPhases.BEFORE_DELETE, context);
        handler.execute(entity, definition, module, CoredeuxHookPhases.BEFORE_REFRESH, context);
        handler.execute(entity, definition, module, CoredeuxHookPhases.AFTER_REFRESH, context);

        assertEquals(8, hook.invocationCount);
    }

    @Test
    void shouldFailForUnsupportedPhase() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        applicationContext.registerSingleton("lifecycleHook", new RecordingHook());
        HooksModuleHandler handler = new HooksModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                .handlers(List.of("lifecycleHook")).build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(new SampleEntity(), definition, module, "UNKNOWN", OperationContext.empty()));

        assertTrue(exception.getMessage().contains("Unsupported hook phase"));
    }

    @Test
    void shouldFailWhenHookBeanCannotBeResolved() {
        HooksModuleHandler handler = new HooksModuleHandler(new TestComponentRegistry());
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                .handlers(List.of("missingHook")).build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(new SampleEntity(), definition, module, CoredeuxHookPhases.LOAD,
                        OperationContext.empty()));

        assertTrue(exception.getMessage().contains("missingHook"));
    }

    @Test
    void shouldFailWhenHookDoesNotSupportEntityType() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        applicationContext.registerSingleton("typedHook", new TypedHook());
        HooksModuleHandler handler = new HooksModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                .handlers(List.of("typedHook")).build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(new OtherEntity(), definition, module, CoredeuxHookPhases.LOAD,
                        OperationContext.empty()));

        assertTrue(exception.getMessage().contains("does not support entity type"));
    }

    private static final class SampleEntity {
    }

    private static final class RecordingHook implements CoredeuxEntityHook<SampleEntity> {

        private int invocationCount;

        @Override
        public void onLoad(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
        }

        @Override
        public void beforeSave(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
        }

        @Override
        public void afterSave(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
        }

        @Override
        public void beforeUpdate(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
        }

        @Override
        public void afterUpdate(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
        }

        @Override
        public void beforeDelete(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
        }

        @Override
        public void beforeRefresh(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
        }

        @Override
        public void afterRefresh(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
        }
    }

    private static final class TypedHook implements CoredeuxEntityHook<SampleEntity> {
    }

    private static final class OtherEntity {
    }
}
