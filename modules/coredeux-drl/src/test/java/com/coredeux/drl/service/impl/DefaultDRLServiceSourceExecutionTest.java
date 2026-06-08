package com.coredeux.drl.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.source.resolver.DRLSourceResolver;

import org.junit.jupiter.api.Test;

class DefaultDRLServiceSourceExecutionTest {

    private static final String INLINE_SOURCE = """
            global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

            rule "sample-fact-rule"
            when
                $context : com.coredeux.drl.model.RuleContext(method == "sample-fact-rule")
                $demoService : com.coredeux.drl.service.impl.DemoService()
            then
                $context.setOutput($demoService.message());
            end
            """;

    @Test
    void executesInlineSourceWithoutLoadingFromResolverOrUsingTheCache() {
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            DrlRuntimeBootstrap.initialize();

            CountingResolver resolver = new CountingResolver();
            DefaultDRLService service = new DefaultDRLService(resolver);

            RuleContext context = RuleContext.method("sample-fact-rule")
                    .fact(new DemoService());

            service.executeSource(INLINE_SOURCE, context);

            assertEquals("ok", context.getOutput());
            assertEquals(1, context.getFiredRules());
            assertEquals(0, resolver.resolveCount.get());
            assertEquals(false, service.isCached("sample-fact-rule"));
        } finally {
            restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, previousCompiler);
            restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, previousLanguageLevel);
        }
    }

    @Test
    void compilesCachesAndExecutesProvidedSourceByRuleId() {
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            DrlRuntimeBootstrap.initialize();

            CountingResolver resolver = new CountingResolver();
            DefaultDRLService service = new DefaultDRLService(resolver);

            RuleContext first = RuleContext.method("sample-fact-rule")
                    .fact(new DemoService());
            service.execute("sample-fact-rule", INLINE_SOURCE, first);

            assertEquals("ok", first.getOutput());
            assertEquals(1, first.getFiredRules());
            assertTrue(service.isCached("sample-fact-rule"));
            assertEquals(0, resolver.resolveCount.get());

            RuleContext second = RuleContext.method("sample-fact-rule")
                    .fact(new DemoService());
            service.execute("sample-fact-rule", second);

            assertEquals("ok", second.getOutput());
            assertEquals(1, second.getFiredRules());
            assertEquals(0, resolver.resolveCount.get());
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

    private static final class CountingResolver implements DRLSourceResolver {

        private final AtomicInteger resolveCount = new AtomicInteger();

        @Override
        public String resolve(String ruleId) {
            resolveCount.incrementAndGet();
            throw new IllegalStateException("Resolver should not be called for source-based execution: " + ruleId);
        }
    }
}
