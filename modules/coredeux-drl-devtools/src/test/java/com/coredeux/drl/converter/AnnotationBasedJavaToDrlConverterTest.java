package com.coredeux.drl.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.impl.DefaultDRLService;

import org.junit.jupiter.api.Test;

class AnnotationBasedJavaToDrlConverterTest {

    private final AnnotationBasedJavaToDrlConverter converter = new AnnotationBasedJavaToDrlConverter();

    @Test
    void convertsAnnotatedJavaSourceIntoExecutableDrl() {
        withDroolsDefaults(() -> {
            ConvertedDrl converted = converter.convert("""
                    package rules;

                    import com.coredeux.core.registry.CoredeuxComponentRegistry;
                    import com.coredeux.drl.converter.annotations.DrlDefinition;
                    import com.coredeux.drl.converter.annotations.DrlGlobal;
                    import com.coredeux.drl.converter.annotations.DrlRule;
                    import com.coredeux.drl.model.RuleContext;
                    import com.coredeux.drl.converter.DemoService;

                    @DrlDefinition("demoRuleSource")
                    public class DemoRuleSource {

                        @DrlGlobal
                        public CoredeuxComponentRegistry componentRegistry;

                        @DrlRule(name = "checkDemo", when = "$context : RuleContext(method == 'checkDemo')")
                        public void checkDemo(RuleContext $context) {
                            DemoService demoService = componentRegistry.getComponent("demoService", DemoService.class);
                            $context.setOutput(demoService.message());
                        }
                    }
                    """);

            assertEquals("demoRuleSource", converted.getRuleId());
            assertTrue(converted.getDrl().contains("import java.lang.*;"));
            assertTrue(converted.getDrl().contains("global CoredeuxComponentRegistry componentRegistry;"));
            assertTrue(converted.getDrl().contains("rule \"checkDemo\""));
            assertTrue(converted.getDrl().contains("$context : RuleContext(method == 'checkDemo')"));
            assertTrue(converted.getDrl().contains("DemoService demoService = componentRegistry.getComponent"));
            assertTrue(!converted.getDrl().contains("converter.annotations"));

            CoredeuxComponentRegistry componentRegistry = InMemoryCoredeuxComponentRegistry.builder()
                    .component("demoService", new DemoService())
                    .build();
            DefaultDRLService service = new DefaultDRLService(ruleId -> converted.getDrl(), componentRegistry);

            RuleContext context = RuleContext.method("checkDemo");
            service.execute(converted.getRuleId(), context);

            assertEquals("ok", context.getOutput());
            assertEquals(1, context.getFiredRules());
        });
    }

    @Test
    void supportsMultipleRulesInOneAnnotatedSource() {
        ConvertedDrl converted = converter.convert("""
                package rules;

                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;

                @DrlDefinition("multiRuleSource")
                public class MultiRuleSource {

                    @DrlRule(name = "first", when = "$context : RuleContext(method == 'first')")
                    public void first(RuleContext $context) {
                        $context.setOutput("first");
                    }

                    @DrlRule(name = "second", when = "$context : RuleContext(method == 'second')")
                    public void second(RuleContext $context) {
                        $context.setOutput("second");
                    }
                }
                """);

        assertTrue(converted.getDrl().contains("rule \"first\""));
        assertTrue(converted.getDrl().contains("rule \"second\""));
    }

    @Test
    void failsWhenRuleHasNoWhenCondition() {
        DrlConversionException exception = assertThrows(DrlConversionException.class, () -> converter.convert("""
                package rules;

                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;

                @DrlDefinition("broken")
                public class BrokenRuleSource {

                    @DrlRule("brokenRule")
                    public void brokenRule(RuleContext $context) {
                        $context.setOutput("broken");
                    }
                }
                """));

        assertTrue(exception.getMessage().contains("@DrlRule requires when"));
    }

    private void withDroolsDefaults(Runnable runnable) {
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            DrlRuntimeBootstrap.initialize();
            runnable.run();
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
