package com.coredeux.drl.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

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

    @Test
    void failsWhenDefinitionIsMissing() {
        DrlConversionException exception = assertThrows(DrlConversionException.class, () -> converter.convert("""
                package rules;

                public class MissingDefinition {

                    public void helper() {
                    }
                }
                """));

        assertTrue(exception.getMessage().contains("Missing @DrlDefinition on source class"));
    }

    @Test
    void failsWhenThereIsMoreThanOneDefinitionClass() {
        DrlConversionException exception = assertThrows(DrlConversionException.class, () -> converter.convert("""
                package rules;

                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;

                @DrlDefinition("first")
                class FirstDefinition {

                    @DrlRule("firstRule")
                    public void firstRule(RuleContext $context) {
                        $context.setOutput("first");
                    }
                }

                @DrlDefinition("second")
                class SecondDefinition {

                    @DrlRule("secondRule")
                    public void secondRule(RuleContext $context) {
                        $context.setOutput("second");
                    }
                }
                """));

        assertTrue(exception.getMessage().contains("Only one @DrlDefinition class is supported per source file"));
    }

    @Test
    void failsWhenDefinitionValueIsBlank() {
        DrlConversionException exception = assertThrows(DrlConversionException.class, () -> converter.convert("""
                package rules;

                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;

                @DrlDefinition("   ")
                public class BlankDefinition {

                    @DrlRule("blankRule")
                    public void blankRule(RuleContext $context) {
                        $context.setOutput("blank");
                    }
                }
                """));

        assertTrue(exception.getMessage().contains("@DrlDefinition value must not be blank"));
    }

    @Test
    void failsWhenNoRulesAreDeclared() {
        DrlConversionException exception = assertThrows(DrlConversionException.class, () -> converter.convert("""
                package rules;

                import com.coredeux.core.registry.CoredeuxComponentRegistry;
                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlGlobal;

                @DrlDefinition("empty")
                public class EmptyRuleSource {

                    @DrlGlobal
                    public CoredeuxComponentRegistry componentRegistry;
                }
                """));

        assertTrue(exception.getMessage().contains("At least one @DrlRule method is required"));
    }

    @Test
    void failsWhenSourceCannotBeParsed() {
        DrlConversionException exception = assertThrows(DrlConversionException.class,
                () -> converter.convert("package rules; class Broken {"));

        assertTrue(exception.getMessage().contains("Unable to parse Java DRL source"));
        assertNotNull(exception.getCause());
    }

    @Test
    void supportsNormalAnnotationValuesAndEmptyRuleBodies() {
        ConvertedDrl converted = converter.convert("""
                package rules;

                import com.coredeux.drl.converter.annotations.DrlDefinition;
                import com.coredeux.drl.converter.annotations.DrlRule;
                import com.coredeux.drl.model.RuleContext;

                @DrlDefinition("valueRuleSource")
                public class ValueRuleSource {

                    @DrlRule(value = "valueRule", when = "$context : RuleContext(method == 'valueRule')")
                    public void valueRule(RuleContext $context) {
                    }
                }
                """);

        assertEquals("valueRuleSource", converted.getRuleId());
        assertTrue(converted.getDrl().contains("rule \"valueRule\""));
        assertTrue(converted.getDrl().contains("when"));
        assertTrue(converted.getDrl().contains("then"));
        assertTrue(converted.getDrl().contains("end"));
        assertTrue(!converted.getDrl().contains("valueRule("));
    }

    private void withDroolsDefaults(Runnable runnable) {
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            resetBootstrap();
            DrlRuntimeBootstrap.initialize();
            runnable.run();
        } finally {
            restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, previousCompiler);
            restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, previousLanguageLevel);
            resetBootstrap();
        }
    }

    private void restoreProperty(String name, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previousValue);
        }
    }

    private void resetBootstrap() {
        try {
            Field field = DrlRuntimeBootstrap.class.getDeclaredField("INITIALIZED");
            field.setAccessible(true);
            ((java.util.concurrent.atomic.AtomicBoolean) field.get(null)).set(false);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to reset bootstrap state", exception);
        }
    }
}
