package com.coredeux.drl.core.handler.service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

import com.coredeux.core.exceptions.CoredeuxValueHandlerException;
import com.coredeux.core.handler.ValueContext;
import com.coredeux.core.handler.service.CoredeuxValueHandlerService;
import com.coredeux.core.handler.service.impl.DefaultCoredeuxValueHandlerService;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;

/**
 * Value handler service that routes plain Java handlers through the core
 * registry and routes {@code .drl} handlers through the DRL runtime.
 */
public class DefaultDRLCoredeuxValueHandlerService extends DefaultCoredeuxValueHandlerService
        implements CoredeuxValueHandlerService {

    private static final String DRL_SUFFIX = ".drl";
    private static final String DRL_METHOD = "handle";

    private final DRLService drlService;

    public DefaultDRLCoredeuxValueHandlerService(CoredeuxComponentRegistry componentRegistry, DRLService drlService) {
        super(componentRegistry);
        this.drlService = Objects.requireNonNull(drlService, "drlService");
    }

    @Override
    public <T, V extends ValueContext> T invoke(String handler, V context) {
        String handlerName = resolveHandlerName(handler);
        if (!isDrlHandler(handlerName)) {
            return super.invoke(handlerName, context);
        }

        RuleContext<T> ruleContext = toRuleContext(context);
        drlService.execute(handlerName, ruleContext);
        return ruleContext.getOutput();
    }

    private boolean isDrlHandler(String handlerName) {
        return handlerName.endsWith(DRL_SUFFIX);
    }

    private <T, V extends ValueContext> RuleContext<T> toRuleContext(V context) {
        if (context == null) {
            throw new CoredeuxValueHandlerException("Value handler context must not be null");
        }
        RuleContext<T> ruleContext = RuleContext.method(DRL_METHOD);
        ruleContext.param("context", context);
        ruleContext.fact(context);
        copyContextFields(ruleContext, context);
        return ruleContext;
    }

    private <T, V extends ValueContext> void copyContextFields(RuleContext<T> ruleContext, V context) {
        Class<?> type = context.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    ruleContext.param(field.getName(), field.get(context));
                } catch (IllegalAccessException exception) {
                    throw new CoredeuxValueHandlerException(
                            "Unable to read value handler context field: " + field.getName(), exception);
                }
            }
            type = type.getSuperclass();
        }
    }
}
