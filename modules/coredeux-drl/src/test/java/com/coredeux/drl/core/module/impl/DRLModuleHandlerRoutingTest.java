package com.coredeux.drl.core.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
