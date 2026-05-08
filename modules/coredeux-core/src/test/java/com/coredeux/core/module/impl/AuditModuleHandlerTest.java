package com.coredeux.core.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

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
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        applicationContext.getBeanFactory().registerSingleton("entityAuditHandler", auditHandler);
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
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        applicationContext.getBeanFactory().registerSingleton("entityAuditHandler", auditHandler);
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
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        applicationContext.getBeanFactory().registerSingleton("entityAuditHandler", auditHandler);
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
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        applicationContext.getBeanFactory().registerSingleton("entityAuditHandler", auditHandler);
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
        AuditModuleHandler handler = new AuditModuleHandler(new StaticApplicationContext());
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
        AuditModuleHandler handler = new AuditModuleHandler(new StaticApplicationContext());
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
        AuditModuleHandler handler = new AuditModuleHandler(new StaticApplicationContext());
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
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("typedAuditHandler", new TypedAuditHandler());
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
