package com.coredeux.drl.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.source.resolver.DRLSourceResolver;

import org.junit.jupiter.api.Test;

class DefaultDRLServiceComponentRegistryTest {

    @Test
    void exposesComponentRegistryAsAGlobalToRules() {
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            DrlRuntimeBootstrap.initialize();

            DRLSourceResolver resolver = ruleId -> """
                    global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

                    rule "check"
                    when
                        $context : com.coredeux.drl.model.RuleContext(method == "check")
                    then
                        com.coredeux.drl.service.impl.DemoService demoService =
                                componentRegistry.getComponent("demoService",
                                        com.coredeux.drl.service.impl.DemoService.class);
                        $context.setOutput(demoService.message());
                    end
                    """;

            CoredeuxComponentRegistry componentRegistry = InMemoryCoredeuxComponentRegistry.builder()
                    .component("demoService", new DemoService())
                    .build();

            DefaultDRLService service = new DefaultDRLService(resolver, componentRegistry);

            RuleContext context = RuleContext.method("check");
            service.execute("check", context);

            assertEquals("ok", context.getOutput());
            assertEquals(1, context.getFiredRules());
        } finally {
            restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, previousCompiler);
            restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, previousLanguageLevel);
        }
    }

    private void restoreProperty(String name, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previousValue);
        }
    }

}
