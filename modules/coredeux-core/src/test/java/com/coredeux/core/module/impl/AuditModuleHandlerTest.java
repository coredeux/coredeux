package com.coredeux.core.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import com.coredeux.core.testsupport.TestComponentRegistry;

import com.coredeux.core.audit.CoredeuxEntityAuditHandler;
import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;

class AuditModuleHandlerTest {

    @Test
    void shouldExecuteAuditHandlerForConfiguredSaveOperation() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        applicationContext.registerSingleton("entityAuditHandler", auditHandler);
        AuditModuleHandler handler = new AuditModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                .handlers(List.of("entityAuditHandler"))
                .config(Map.of("operations", List.of("save")))
                .build();
        OperationContext context = OperationContext.builder()
                .lifecycleContext(EntityLifecycleContext.builder().operation(CoredeuxLifecycleOperations.UPSERT).build())
                .build();

        handler.execute(new SampleEntity(), definition, module, CoredeuxHookPhases.AFTER_SAVE, context);

        assertEquals(1, auditHandler.invocationCount);
        assertEquals(CoredeuxLifecycleOperations.UPSERT, auditHandler.context.getLifecycleContext().getOperation());
    }

    @Test
    void shouldExecuteAuditHandlerForAllOperations() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        applicationContext.registerSingleton("entityAuditHandler", auditHandler);
        AuditModuleHandler handler = new AuditModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                .handlers(List.of("entityAuditHandler"))
                .config(Map.of("operations", List.of("ALL")))
                .build();

        handler.execute(new SampleEntity(), definition, module, CoredeuxHookPhases.BEFORE_DELETE, OperationContext.empty());

        assertEquals(1, auditHandler.invocationCount);
    }

    @Test
    void shouldSkipWhenPhaseDoesNotMapToAuditOperation() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        applicationContext.registerSingleton("entityAuditHandler", auditHandler);
        AuditModuleHandler handler = new AuditModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                .handlers(List.of("entityAuditHandler"))
                .config(Map.of("operations", List.of("ALL")))
                .build();

        handler.execute(new SampleEntity(), definition, module, CoredeuxHookPhases.BEFORE_SAVE, OperationContext.empty());

        assertEquals(0, auditHandler.invocationCount);
    }

    @Test
    void shouldSkipWhenOperationIsNotConfigured() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        applicationContext.registerSingleton("entityAuditHandler", auditHandler);
        AuditModuleHandler handler = new AuditModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                .handlers(List.of("entityAuditHandler"))
                .config(Map.of("operations", List.of("SAVE")))
                .build();

        handler.execute(new SampleEntity(), definition, module, CoredeuxHookPhases.AFTER_UPDATE, OperationContext.empty());

        assertEquals(0, auditHandler.invocationCount);
    }

    @Test
    void shouldFailWhenAuditHandlerBeanCannotBeResolved() {
        AuditModuleHandler handler = new AuditModuleHandler(new TestComponentRegistry());
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                .handlers(List.of("missingAuditHandler"))
                .config(Map.of("operations", List.of("SAVE")))
                .build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(new SampleEntity(), definition, module, CoredeuxHookPhases.AFTER_SAVE,
                        OperationContext.empty()));

        assertTrue(exception.getMessage().contains("missingAuditHandler"));
    }

    @Test
    void shouldFailWhenAuditOperationsAreMissing() {
        AuditModuleHandler handler = new AuditModuleHandler(new TestComponentRegistry());
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                .handlers(List.of("entityAuditHandler"))
                .config(Map.of())
                .build();

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> handler.execute(new SampleEntity(), definition, module, CoredeuxHookPhases.AFTER_SAVE,
                        OperationContext.empty()));

        assertTrue(exception.getMessage().contains("config.operations"));
    }

    @Test
    void shouldFailWhenAuditOperationIsInvalid() {
        AuditModuleHandler handler = new AuditModuleHandler(new TestComponentRegistry());
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                .handlers(List.of("entityAuditHandler"))
                .config(Map.of("operations", List.of("ARCHIVE")))
                .build();

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> handler.execute(new SampleEntity(), definition, module, CoredeuxHookPhases.AFTER_SAVE,
                        OperationContext.empty()));

        assertTrue(exception.getMessage().contains("ARCHIVE"));
    }

    @Test
    void shouldFailWhenAuditHandlerDoesNotSupportEntityType() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        applicationContext.registerSingleton("typedAuditHandler", new TypedAuditHandler());
        AuditModuleHandler handler = new AuditModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                .handlers(List.of("typedAuditHandler"))
                .config(Map.of("operations", List.of("SAVE")))
                .build();

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> handler.execute(new OtherEntity(), definition, module, CoredeuxHookPhases.AFTER_SAVE,
                        OperationContext.empty()));

        assertTrue(exception.getMessage().contains("does not support entity type"));
    }

    @Test
    void shouldSkipBlankAuditHandlers() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        applicationContext.registerSingleton("entityAuditHandler", auditHandler);
        AuditModuleHandler handler = new AuditModuleHandler(applicationContext);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().fullClassName("sample.Type").build();
        List<String> handlers = new ArrayList<>();
        handlers.add(" ");
        handlers.add(null);
        handlers.add("entityAuditHandler");
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                .handlers(handlers)
                .config(Map.of("operations", List.of("SAVE")))
                .build();

        handler.execute(new SampleEntity(), definition, module, CoredeuxHookPhases.AFTER_SAVE, OperationContext.empty());

        assertEquals(1, auditHandler.invocationCount);
    }

    private static final class SampleEntity {
    }

    private static final class OtherEntity {
    }

    private static final class RecordingAuditHandler implements CoredeuxEntityAuditHandler<SampleEntity> {

        private int invocationCount;
        private OperationContext context;

        @Override
        public void audit(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
            this.context = context;
        }
    }

    private static final class TypedAuditHandler implements CoredeuxEntityAuditHandler<SampleEntity> {

        @Override
        public void audit(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
        }
    }
}
