package com.coredeux.drl.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.source.resolver.impl.ClasspathDRLSourceResolver;

import org.junit.jupiter.api.Test;

class DefaultDRLServiceTest {

    @Test
    void resolvesRuleByIdAndExecutesIt() {
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            DrlRuntimeBootstrap.initialize();

            DefaultDRLService service = new DefaultDRLService(
                    new ClasspathDRLSourceResolver(getClass().getClassLoader(), "rules/", ".drl"));

            RuleContext context = RuleContext.method("sample-rule");
            service.execute("sample-rule", context);

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
