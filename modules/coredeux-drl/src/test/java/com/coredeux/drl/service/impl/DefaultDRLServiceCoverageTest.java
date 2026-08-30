package com.coredeux.drl.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.drl.exceptions.CoredeuxDRLException;
import com.coredeux.drl.cache.CompiledDRLRule;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.source.resolver.DRLSourceResolver;

class DefaultDRLServiceCoverageTest {

    @Test
    void shouldExecuteResolvedRulesAndReuseTheCache() {
        DRLSourceResolver resolver = mock(DRLSourceResolver.class);
        when(resolver.resolve("sample-rule")).thenReturn("""
                package rules.sample;

                import com.coredeux.drl.model.RuleContext;

                rule "resolverRule"
                when
                    $context : RuleContext(method == "resolver")
                then
                    $context.setOutput("resolver-ok");
                    $context.setMessage("resolver");
                end
                """);

        DefaultDRLService service = new DefaultDRLService(resolver);
        RuleContext context = RuleContext.method("resolver").fact("ignored");

        service.execute("sample-rule", context);
        service.execute("sample-rule", RuleContext.method("resolver"));

        assertEquals("resolver-ok", context.getOutput());
        assertEquals("resolver", context.getMessage());
        assertEquals(1, context.getFiredRules());
        assertTrue(service.isCached("sample-rule"));
        verify(resolver, times(1)).resolve("sample-rule");
    }

    @Test
    void shouldExecuteSourceAndOnlyInjectComponentRegistryWhenDeclared() {
        CoredeuxComponentRegistry registry = new InMemoryCoredeuxComponentRegistry(Map.of("sampleMessage", "hello"));
        DefaultDRLService service = new DefaultDRLService(mock(DRLSourceResolver.class), registry);

        RuleContext inline = RuleContext.method("inline");
        service.executeSource("""
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;

                rule "inlineRule"
                when
                    $context : RuleContext(method == "inline")
                then
                    $context.setOutput("inline-ok");
                    $context.setMessage("inline");
                end
                """, inline);

        RuleContext global = RuleContext.method("with-global");
        service.executeSource("""
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;
                global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

                rule "globalRule"
                when
                    $context : RuleContext(method == "with-global")
                then
                    $context.setOutput(componentRegistry.getComponent("sampleMessage", String.class));
                    $context.setMessage("global");
                end
                """, global);

        assertEquals("inline-ok", inline.getOutput());
        assertEquals("inline", inline.getMessage());
        assertEquals("hello", global.getOutput());
        assertEquals("global", global.getMessage());
    }

    @Test
    void shouldCompileInlineSourcesUnderRuleIdsAndPurgeCache() {
        DefaultDRLService service = new DefaultDRLService(mock(DRLSourceResolver.class));

        service.execute("inline-rule", """
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;

                rule "inlineRule"
                when
                    $context : RuleContext(method == "inline")
                then
                    $context.setOutput("cached");
                    $context.setMessage("compiled");
                end
                """, RuleContext.method("inline"));

        assertTrue(service.isCached("inline-rule"));
        service.purgeCache("inline-rule");
        assertFalse(service.isCached("inline-rule"));
        service.execute("inline-rule", """
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;

                rule "inlineRule"
                when
                    $context : RuleContext(method == "inline")
                then
                    $context.setOutput("cached");
                    $context.setMessage("compiled");
                end
                """, RuleContext.method("inline"));
        service.purgeCache();
        assertFalse(service.isCached("inline-rule"));
    }

    @Test
    void shouldSurfaceRuleFailuresAsDrlExceptions() {
        DefaultDRLService service = new DefaultDRLService(mock(DRLSourceResolver.class));

        RuleContext context = RuleContext.method("fail");
        assertThrows(CoredeuxDRLException.class, () -> service.executeSource("""
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;

                rule "failingRule"
                when
                    $context : RuleContext(method == "fail")
                then
                    $context.setException(new RuntimeException("boom"));
                end
                """, context));
    }

    @Test
    void shouldFailWhenDrlMatchesNoRules() {
        DefaultDRLService service = new DefaultDRLService(mock(DRLSourceResolver.class));
        RuleContext context = RuleContext.method("missing");

        CoredeuxDRLException exception = assertThrows(CoredeuxDRLException.class, () -> service.executeSource("""
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;

                rule "nonMatchingRule"
                when
                    $context : RuleContext(method == "different")
                then
                    $context.setOutput("should-not-run");
                end
                """, context));

        assertEquals(0, context.getFiredRules());
        assertTrue(exception.getMessage().contains("inline source"));
        assertTrue(exception.getMessage().contains("missing"));
        assertTrue(exception.getMessage().contains("did not match any rule"));
        assertTrue(exception.getMessage().contains("Fired rules: 0"));
    }

    @Test
    void shouldFailWhenDrlMatchesMoreThanOneRule() {
        DefaultDRLService service = new DefaultDRLService(mock(DRLSourceResolver.class));
        RuleContext context = RuleContext.method("duplicate");

        CoredeuxDRLException exception = assertThrows(CoredeuxDRLException.class, () -> service.executeSource("""
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;

                rule "duplicateRuleOne"
                when
                    $context : RuleContext(method == "duplicate")
                then
                    $context.setMessage("first");
                end

                rule "duplicateRuleTwo"
                when
                    $context : RuleContext(method == "duplicate")
                then
                    $context.setOutput("second");
                end
                """, context));

        assertEquals(2, context.getFiredRules());
        assertTrue(exception.getMessage().contains("inline source"));
        assertTrue(exception.getMessage().contains("duplicate"));
        assertTrue(exception.getMessage().contains("matched multiple rules"));
        assertTrue(exception.getMessage().contains("Fired rules: 2"));
    }

    @Test
    void shouldFailWhenExecutionContextIsNull() {
        DefaultDRLService service = new DefaultDRLService(mock(DRLSourceResolver.class));

        CoredeuxDRLException exception = assertThrows(CoredeuxDRLException.class, () -> service.executeSource("""
                package rules.inline;

                rule "anyRule"
                when
                    eval(true)
                then
                end
                """, null));

        assertTrue(exception.getMessage().contains("inline source"));
        assertTrue(exception.getMessage().contains("non-null RuleContext"));
    }

    @Test
    void shouldFailWhenContextMethodIsBlank() {
        DefaultDRLService service = new DefaultDRLService(mock(DRLSourceResolver.class));
        RuleContext context = RuleContext.method("   ");

        CoredeuxDRLException exception = assertThrows(CoredeuxDRLException.class, () -> service.executeSource("""
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;

                rule "blankMethodRule"
                when
                    $context : RuleContext(method == "blank")
                then
                    $context.setOutput("should-not-run");
                end
                """, context));

        assertEquals(0, context.getFiredRules());
        assertTrue(exception.getMessage().contains("blank method name"));
    }

    @Test
    void shouldFailWhenContextMethodIsMissing() {
        DefaultDRLService service = new DefaultDRLService(mock(DRLSourceResolver.class));
        RuleContext<String> context = new RuleContext<>();

        CoredeuxDRLException exception = assertThrows(CoredeuxDRLException.class, () -> service.executeSource("""
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;

                rule "missingMethodRule"
                when
                    $context : RuleContext()
                then
                    $context.setOutput("should-not-run");
                end
                """, context));

        assertEquals(0, context.getFiredRules());
        assertTrue(exception.getMessage().contains("blank method name"));
    }

    @Test
    void shouldCompileAndCacheResolvedAndInlineRules() {
        DRLSourceResolver resolver = mock(DRLSourceResolver.class);
        when(resolver.resolve("resolved-rule")).thenReturn("""
                package rules.resolved;

                import com.coredeux.drl.model.RuleContext;

                rule "resolvedRule"
                when
                    $context : RuleContext(method == "resolved")
                then
                    $context.setOutput("resolved-ok");
                end
                """);

        DefaultDRLService service = new DefaultDRLService(resolver,
                new InMemoryCoredeuxComponentRegistry(Map.of("sampleMessage", "hello")));

        service.compileAndCache("resolved-rule");
        CompiledDRLRule resolved = service.compile("resolved-rule");
        assertNotNull(resolved.kieBase());
        assertTrue(service.isCached("resolved-rule"));

        service.compileAndCache("inline-rule", """
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;
                global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

                rule "inlineRule"
                when
                    $context : RuleContext(method == "inline")
                then
                    $context.setOutput(componentRegistry.getComponent("sampleMessage", String.class));
                end
                """);
        CompiledDRLRule inline = service.compile("inline-rule", """
                package rules.inline;

                import com.coredeux.drl.model.RuleContext;

                rule "inlineRule"
                when
                    $context : RuleContext(method == "inline")
                then
                    $context.setOutput("inline-ok");
                end
                """);

        assertNotNull(inline.kieBase());
        assertTrue(service.isCached("inline-rule"));
    }

    @Test
    void shouldRejectInvalidDrlDuringCompile() {
        DefaultDRLService service = new DefaultDRLService(mock(DRLSourceResolver.class));

        assertThrows(IllegalStateException.class,
                () -> service.compile("bad-rule", """
                        package rules.bad;

                        rule "bad"
                        when
                            $context : com.coredeux.drl.model.RuleContext(method == "bad")
                        then
                            $context.setOutput("broken")
                        """));
    }
}
