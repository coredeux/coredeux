package com.coredeux.drl.core.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.audit.CoredeuxEntityAuditHandler;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.validation.CoredeuxEntityValidator;
import com.coredeux.core.validation.ValidationError;
import com.coredeux.drl.exceptions.CoredeuxDRLException;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;

class DRLModuleHandlerRoutingTest {

    @Test
    void validatorsSplitJavaAndDrlHandlers() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityValidator<Object> validator = mock(CoredeuxEntityValidator.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        when(componentRegistry.getComponent(eq("javaValidator"), eq(CoredeuxEntityValidator.class))).thenReturn(validator);
        when(validator.validate(any(), any(), any())).thenReturn(List.of());

        DRLValidatorsModuleHandler handler = new DRLValidatorsModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("validators")
                .enabled(true)
                .handlers(List.of("javaValidator", "drlValidator.drl"))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_SAVE, context);

        verify(validator, times(1)).validate(eq("customer"), eq(definition), eq(context));
        verify(drlService, times(1)).execute(eq("drlValidator.drl"), any(RuleContext.class));
    }

    @Test
    void validatorsSkipUnsupportedPhasesAndBlankHandlers() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityValidator<Object> validator = mock(CoredeuxEntityValidator.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        when(componentRegistry.getComponent(eq("javaValidator"), eq(CoredeuxEntityValidator.class))).thenReturn(validator);

        DRLValidatorsModuleHandler handler = new DRLValidatorsModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("validators")
                .enabled(true)
                .handlers(List.of(" ", "javaValidator", "drlValidator.drl"))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_SAVE, context);

        verify(componentRegistry, never()).getComponent(eq("javaValidator"), eq(CoredeuxEntityValidator.class));
        verify(drlService, never()).execute(eq("drlValidator.drl"), any(RuleContext.class));
    }

    @Test
    void validatorsRouteBlankSkippedAndNullOutputsAsEmptyLists() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityValidator<Object> validator = mock(CoredeuxEntityValidator.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        when(componentRegistry.getComponent(eq("javaValidator"), eq(CoredeuxEntityValidator.class))).thenReturn(validator);
        when(validator.validate(any(), any(), any())).thenReturn(null);

        DRLValidatorsModuleHandler handler = new DRLValidatorsModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("validators")
                .enabled(true)
                .handlers(List.of(" ", "javaValidator", "drlValidator.drl"))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_SAVE, context);

        verify(validator, times(1)).validate(eq("customer"), eq(definition), eq(context));
        verify(drlService, times(1)).execute(eq("drlValidator.drl"), any(RuleContext.class));
    }

    @Test
    void validatorsThrowWhenAnyValidatorReturnsErrors() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityValidator<Object> validator = mock(CoredeuxEntityValidator.class);

        when(componentRegistry.getComponent(eq("javaValidator"), eq(CoredeuxEntityValidator.class))).thenReturn(validator);
        when(validator.validate(any(), any(), any())).thenReturn(List.of(ValidationError.builder()
                .field("status")
                .message("required")
                .build()));

        DRLValidatorsModuleHandler handler = new DRLValidatorsModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("validators")
                .enabled(true)
                .handlers(List.of("javaValidator"))
                .build();
        OperationContext context = OperationContext.builder().build();

        assertThrows(com.coredeux.core.exceptions.CoredeuxValidationException.class,
                () -> handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_SAVE,
                        context));
    }

    @Test
    void validatorsWrapJavaResolutionAndDrlExecutionFailures() {
        CoredeuxComponentRegistry missingJavaComponentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLValidatorsModuleHandler missingJavaHandler = new DRLValidatorsModuleHandler(missingJavaComponentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition javaModuleDefinition = CoredeuxModuleDefinition.builder()
                .name("validators")
                .enabled(true)
                .handlers(List.of("javaValidator"))
                .build();
        OperationContext context = OperationContext.builder().build();

        assertThrows(com.coredeux.core.exceptions.CoredeuxStrategyException.class,
                () -> missingJavaHandler.execute("customer", definition, javaModuleDefinition,
                        CoredeuxHookPhases.BEFORE_SAVE, context));

        CoredeuxComponentRegistry drlComponentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        when(drlComponentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(drlService).execute(eq("drlValidator.drl"), any(RuleContext.class));

        DRLValidatorsModuleHandler drlHandler = new DRLValidatorsModuleHandler(drlComponentRegistry);
        CoredeuxModuleDefinition drlModuleDefinition = CoredeuxModuleDefinition.builder()
                .name("validators")
                .enabled(true)
                .handlers(List.of("drlValidator.drl"))
                .build();

        assertThrows(com.coredeux.core.exceptions.CoredeuxStrategyException.class,
                () -> drlHandler.execute("customer", definition, drlModuleDefinition, CoredeuxHookPhases.BEFORE_SAVE,
                        context));
    }

    @Test
    void validatorsSurfaceContextExceptionsFromDrlExecution() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RuleContext<List<ValidationError>> context = invocation.getArgument(1, RuleContext.class);
            context.setException(new IllegalStateException("boom"));
            return null;
        }).when(drlService).execute(eq("drlValidator.drl"), any(RuleContext.class));

        DRLValidatorsModuleHandler handler = new DRLValidatorsModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("validators")
                .enabled(true)
                .handlers(List.of("drlValidator.drl"))
                .build();
        OperationContext context = OperationContext.builder().build();

        assertThrows(CoredeuxDRLException.class,
                () -> handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_SAVE,
                        context));
    }

    @Test
    void validatorsRejectIncompatibleBeanTypes() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        CoredeuxEntityValidator validator = new StringValidator();

        when(componentRegistry.getComponent(eq("wrongValidator"), eq(CoredeuxEntityValidator.class))).thenReturn(validator);

        DRLValidatorsModuleHandler handler = new DRLValidatorsModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("validators")
                .enabled(true)
                .handlers(List.of("wrongValidator"))
                .build();
        OperationContext context = OperationContext.builder().build();
        Object entity = new Object();

        assertThrows(com.coredeux.core.exceptions.CoredeuxStrategyException.class,
                () -> handler.execute(entity, definition, moduleDefinition, CoredeuxHookPhases.BEFORE_SAVE,
                        context));
    }

    @Test
    void hooksSplitJavaAndDrlHandlers() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityHook<Object> hook = mock(CoredeuxEntityHook.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        when(componentRegistry.getComponent(eq("javaHook"), eq(CoredeuxEntityHook.class))).thenReturn(hook);

        DRLHooksModuleHandler handler = new DRLHooksModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("hooks")
                .enabled(true)
                .handlers(List.of("javaHook", "drlHook.drl"))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_SAVE, context);

        verify(hook, times(1)).beforeSave(eq("customer"), eq(definition), eq(context));
        verify(drlService, times(1)).execute(eq("drlHook.drl"), any(RuleContext.class));
    }

    @Test
    void hooksSkipBlankHandlersAndUnsupportedDrlPhases() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);

        DRLHooksModuleHandler handler = new DRLHooksModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("hooks")
                .enabled(true)
                .handlers(List.of(" ", "drlHook.drl"))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, "custom-phase", context);

        verify(drlService, never()).execute(eq("drlHook.drl"), any(RuleContext.class));
    }

    @Test
    void hooksRouteEverySupportedPhaseToTheMatchingDrlMethodAndIgnoreCustomPhases() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        List<String> routedMethods = new ArrayList<>();

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RuleContext<Void> context = invocation.getArgument(1, RuleContext.class);
            routedMethods.add(context.getMethod());
            return null;
        }).when(drlService).execute(eq("phaseHook.drl"), any(RuleContext.class));

        DRLHooksModuleHandler handler = new DRLHooksModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("hooks")
                .enabled(true)
                .handlers(List.of(" ", "phaseHook.drl"))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.LOAD, context);
        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_SAVE, context);
        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_SAVE, context);
        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_UPDATE, context);
        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_UPDATE, context);
        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_DELETE, context);
        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_REFRESH, context);
        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_REFRESH, context);
        handler.execute("customer", definition, moduleDefinition, "custom-phase", context);

        assertEquals(List.of("onLoad", "beforeSave", "afterSave", "beforeUpdate", "afterUpdate", "beforeDelete",
                "beforeRefresh", "afterRefresh"), routedMethods);
    }

    @Test
    void hooksWrapJavaResolutionAndDrlExecutionFailures() {
        CoredeuxComponentRegistry missingJavaComponentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLHooksModuleHandler missingJavaHandler = new DRLHooksModuleHandler(missingJavaComponentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition javaModuleDefinition = CoredeuxModuleDefinition.builder()
                .name("hooks")
                .enabled(true)
                .handlers(List.of("javaHook"))
                .build();
        OperationContext context = OperationContext.builder().build();

        assertThrows(com.coredeux.core.exceptions.CoredeuxStrategyException.class,
                () -> missingJavaHandler.execute("customer", definition, javaModuleDefinition,
                        CoredeuxHookPhases.BEFORE_SAVE, context));

        CoredeuxComponentRegistry drlComponentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        when(drlComponentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(drlService).execute(eq("drlHook.drl"), any(RuleContext.class));

        DRLHooksModuleHandler drlHandler = new DRLHooksModuleHandler(drlComponentRegistry);
        CoredeuxModuleDefinition drlModuleDefinition = CoredeuxModuleDefinition.builder()
                .name("hooks")
                .enabled(true)
                .handlers(List.of("drlHook.drl"))
                .build();

        assertThrows(com.coredeux.core.exceptions.CoredeuxStrategyException.class,
                () -> drlHandler.execute("customer", definition, drlModuleDefinition, CoredeuxHookPhases.BEFORE_SAVE,
                        context));
    }

    @Test
    void hooksSurfaceContextExceptionsFromDrlExecution() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RuleContext<Void> context = invocation.getArgument(1, RuleContext.class);
            context.setException(new IllegalStateException("boom"));
            return null;
        }).when(drlService).execute(eq("drlHook.drl"), any(RuleContext.class));

        DRLHooksModuleHandler handler = new DRLHooksModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("hooks")
                .enabled(true)
                .handlers(List.of("drlHook.drl"))
                .build();
        OperationContext context = OperationContext.builder().build();

        assertThrows(CoredeuxDRLException.class,
                () -> handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_SAVE,
                        context));
    }

    @Test
    void auditSplitJavaAndDrlHandlers() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityAuditHandler<Object> auditHandler = mock(CoredeuxEntityAuditHandler.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        when(componentRegistry.getComponent(eq("javaAudit"), eq(CoredeuxEntityAuditHandler.class))).thenReturn(auditHandler);

        DRLAuditModuleHandler handler = new DRLAuditModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("javaAudit", "drlAudit.drl"))
                .config(Map.of("operations", List.of("SAVE")))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_SAVE, context);

        verify(auditHandler, times(1)).audit(eq("customer"), eq(definition), eq(context));
        verify(drlService, times(1)).execute(eq("drlAudit.drl"), any(RuleContext.class));
    }

    @Test
    void auditSkipsWhenOperationsDoNotMatchTheCurrentPhase() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityAuditHandler<Object> auditHandler = mock(CoredeuxEntityAuditHandler.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        when(componentRegistry.getComponent(eq("javaAudit"), eq(CoredeuxEntityAuditHandler.class))).thenReturn(auditHandler);

        DRLAuditModuleHandler handler = new DRLAuditModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("javaAudit", "drlAudit.drl"))
                .config(Map.of("operations", List.of("UPDATE")))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_SAVE, context);

        verify(auditHandler, never()).audit(any(), any(), any());
        verify(drlService, never()).execute(anyString(), any(RuleContext.class));
    }

    @Test
    void auditRoutesSupportedPhasesAndIgnoresCustomPhasesWhileSkippingBlankHandlers() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityAuditHandler<Object> auditHandler = mock(CoredeuxEntityAuditHandler.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        when(componentRegistry.getComponent(eq("javaAudit"), eq(CoredeuxEntityAuditHandler.class))).thenReturn(auditHandler);

        DRLAuditModuleHandler handler = new DRLAuditModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of(" ", "javaAudit", "drlAudit.drl"))
                .config(Map.of("operations", List.of("ALL")))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_SAVE, context);
        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_UPDATE, context);
        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.BEFORE_DELETE, context);
        handler.execute("customer", definition, moduleDefinition, "custom-phase", context);

        verify(auditHandler, times(3)).audit(eq("customer"), eq(definition), eq(context));
        verify(drlService, times(3)).execute(eq("drlAudit.drl"), any(RuleContext.class));
    }

    @Test
    void auditWrapsJavaResolutionAndDrlExecutionFailures() {
        CoredeuxComponentRegistry missingJavaComponentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLAuditModuleHandler missingJavaHandler = new DRLAuditModuleHandler(missingJavaComponentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition javaModuleDefinition = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("javaAudit"))
                .config(Map.of("operations", List.of("ALL")))
                .build();
        OperationContext context = OperationContext.builder().build();

        assertThrows(com.coredeux.core.exceptions.CoredeuxStrategyException.class,
                () -> missingJavaHandler.execute("customer", definition, javaModuleDefinition,
                        CoredeuxHookPhases.AFTER_SAVE, context));

        CoredeuxComponentRegistry drlComponentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        when(drlComponentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(drlService).execute(eq("drlAudit.drl"), any(RuleContext.class));

        DRLAuditModuleHandler drlHandler = new DRLAuditModuleHandler(drlComponentRegistry);
        CoredeuxModuleDefinition drlModuleDefinition = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("drlAudit.drl"))
                .config(Map.of("operations", List.of("ALL")))
                .build();

        assertThrows(com.coredeux.core.exceptions.CoredeuxStrategyException.class,
                () -> drlHandler.execute("customer", definition, drlModuleDefinition, CoredeuxHookPhases.AFTER_SAVE,
                        context));
    }

    @Test
    void auditSurfaceContextExceptionsFromDrlExecution() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityAuditHandler<Object> auditHandler = mock(CoredeuxEntityAuditHandler.class);

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        when(componentRegistry.getComponent(eq("javaAudit"), eq(CoredeuxEntityAuditHandler.class))).thenReturn(auditHandler);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RuleContext<Void> context = invocation.getArgument(1, RuleContext.class);
            context.setException(new IllegalStateException("boom"));
            return null;
        }).when(drlService).execute(eq("drlAudit.drl"), any(RuleContext.class));

        DRLAuditModuleHandler handler = new DRLAuditModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("drlAudit.drl"))
                .config(Map.of("operations", List.of("ALL")))
                .build();
        OperationContext context = OperationContext.builder().build();

        assertThrows(CoredeuxDRLException.class,
                () -> handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_SAVE,
                        context));
    }

    @Test
    void auditRejectsIncompatibleBeanTypes() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        CoredeuxEntityAuditHandler auditHandler = new StringAuditHandler();

        when(componentRegistry.getComponent(eq("wrongAudit"), eq(CoredeuxEntityAuditHandler.class))).thenReturn(auditHandler);

        DRLAuditModuleHandler handler = new DRLAuditModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("wrongAudit"))
                .config(Map.of("operations", List.of("ALL")))
                .build();
        OperationContext context = OperationContext.builder().build();
        Object entity = new Object();

        assertThrows(com.coredeux.core.exceptions.CoredeuxStrategyException.class,
                () -> handler.execute(entity, definition, moduleDefinition, CoredeuxHookPhases.AFTER_SAVE,
                        context));
    }

    @Test
    void moduleNamesAreExposed() {
        assertEquals("hooks", new DRLHooksModuleHandler(mock(CoredeuxComponentRegistry.class)).getModuleName());
        assertEquals("validators", new DRLValidatorsModuleHandler(mock(CoredeuxComponentRegistry.class)).getModuleName());
        assertEquals("audit", new DRLAuditModuleHandler(mock(CoredeuxComponentRegistry.class)).getModuleName());
    }

    @Test
    void auditKeepsConfiguredJavaAndDrlHandlerOrder() {
        CoredeuxComponentRegistry componentRegistry = mock(CoredeuxComponentRegistry.class);
        DRLService drlService = mock(DRLService.class);
        @SuppressWarnings("unchecked")
        CoredeuxEntityAuditHandler<Object> auditHandler = mock(CoredeuxEntityAuditHandler.class);
        List<String> executionOrder = new ArrayList<>();

        doAnswer(invocation -> {
            executionOrder.add("java");
            return null;
        }).when(auditHandler).audit(any(), any(), any());
        doAnswer(invocation -> {
            executionOrder.add("drl");
            return null;
        }).when(drlService).execute(eq("drlAudit.drl"), any(RuleContext.class));

        when(componentRegistry.getComponent(eq("coredeuxDrlService"), eq(DRLService.class))).thenReturn(drlService);
        when(componentRegistry.getComponent(eq("javaAudit"), eq(CoredeuxEntityAuditHandler.class))).thenReturn(auditHandler);

        DRLAuditModuleHandler handler = new DRLAuditModuleHandler(componentRegistry);
        CoredeuxEntityDefinition definition = entityDefinition();
        CoredeuxModuleDefinition moduleDefinition = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("javaAudit", "drlAudit.drl"))
                .config(Map.of("operations", List.of("SAVE")))
                .build();
        OperationContext context = OperationContext.builder().build();

        handler.execute("customer", definition, moduleDefinition, CoredeuxHookPhases.AFTER_SAVE, context);

        assertEquals(List.of("java", "drl"), executionOrder);
    }

    private CoredeuxEntityDefinition entityDefinition() {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName("com.coredeux.demo.domain.Customer")
                .name("customer")
                .identifier("pk")
                .build();
        assertNotNull(definition);
        assertEquals("customer", definition.getName());
        return definition;
    }

    private static final class StringValidator implements CoredeuxEntityValidator<String> {

        @Override
        public List<ValidationError> validate(String entity, CoredeuxEntityDefinition definition,
                OperationContext context) {
            return List.of();
        }
    }

    private static final class StringAuditHandler implements CoredeuxEntityAuditHandler<String> {

        @Override
        public void audit(String entity, CoredeuxEntityDefinition definition, OperationContext context) {
        }
    }
}
