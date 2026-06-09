package com.coredeux.drl.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.source.resolver.DRLSourceResolver;
import com.coredeux.drl.source.resolver.impl.ClasspathDRLSourceResolver;

import org.junit.jupiter.api.Test;

class DefaultDRLServiceCacheTest {

    @Test
    void cachesCompiledKnowledgeBaseByRuleIdAndSupportsPurge() {
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            DrlRuntimeBootstrap.initialize();

            CountingResolver resolver = new CountingResolver(
                    new ClasspathDRLSourceResolver(getClass().getClassLoader(), "rules/", ".drl"));
            DefaultDRLService service = new DefaultDRLService(resolver);

            RuleContext first = RuleContext.method("sample-rule");
            service.execute("sample-rule", first);
            assertEquals(1, resolver.resolveCount.get());
            assertEquals("ok", first.getOutput());

            RuleContext second = RuleContext.method("sample-rule");
            service.execute("sample-rule", second);
            assertEquals(1, resolver.resolveCount.get());
            assertEquals("ok", second.getOutput());

            service.purgeCache("sample-rule");
            assertEquals(false, service.isCached("sample-rule"));

            RuleContext third = RuleContext.method("sample-rule");
            service.execute("sample-rule", third);
            assertEquals(2, resolver.resolveCount.get());
            assertEquals("ok", third.getOutput());

            service.purgeCache();
            assertEquals(false, service.isCached("sample-rule"));
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
        private final DRLSourceResolver delegate;

        private CountingResolver(DRLSourceResolver delegate) {
            this.delegate = delegate;
        }

        @Override
        public String resolve(String ruleId) {
            resolveCount.incrementAndGet();
            return delegate.resolve(ruleId);
        }
    }
}
