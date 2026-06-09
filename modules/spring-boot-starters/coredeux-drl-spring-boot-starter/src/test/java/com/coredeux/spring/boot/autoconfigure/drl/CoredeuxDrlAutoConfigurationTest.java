package com.coredeux.spring.boot.autoconfigure.drl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;
import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;
import com.coredeux.drl.source.resolver.DRLSourceResolver;
import com.coredeux.spring.boot.autoconfigure.CoredeuxAutoConfiguration;
import com.coredeux.spring.boot.autoconfigure.SpringCoredeuxComponentRegistry;

class CoredeuxDrlAutoConfigurationTest {

    private final CoredeuxDrlAutoConfiguration configuration = new CoredeuxDrlAutoConfiguration();
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxAutoConfiguration.class,
                    CoredeuxDrlAutoConfiguration.class))
            .withBean(CoredeuxRequestContextResolver.class, () -> () -> null)
            .withUserConfiguration(SampleServiceConfiguration.class);

    @Test
    void autoConfiguresSpringDrlServiceAndUsesSpringComponentRegistryByDefault() {
        contextRunner.run(context -> {
            assertInstanceOf(SpringCoredeuxComponentRegistry.class,
                    context.getBean(CoredeuxComponentRegistry.class));

            DRLService drlService = context.getBean(DRLService.class);
            RuleContext ruleContext = RuleContext.method("sample-rule");
            drlService.execute("sample-rule", ruleContext);

            assertEquals("ok", ruleContext.getOutput());
            assertEquals("ok", ruleContext.getMessage());
            assertEquals(1, ruleContext.getFiredRules());
        });
    }

    @Test
    void allowsCustomResolverAndKeepsCoreRegistryWiring() {
        contextRunner.withBean(com.coredeux.drl.source.resolver.DRLSourceResolver.class,
                () -> ruleId -> """
                        global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;

                        rule "sample-rule"
                        when
                            $context : com.coredeux.drl.model.RuleContext(method == "sample-rule")
                        then
                            com.coredeux.spring.boot.autoconfigure.drl.SampleService sampleService =
                                    componentRegistry.getComponent("sampleService",
                                            com.coredeux.spring.boot.autoconfigure.drl.SampleService.class);
                            $context.setOutput(sampleService.message());
                            $context.setMessage(sampleService.message());
                        end
                        """)
                .run(context -> {
                    DRLService drlService = context.getBean(DRLService.class);
                    RuleContext ruleContext = RuleContext.method("sample-rule");
                    drlService.execute("sample-rule", ruleContext);

                    assertEquals("ok", ruleContext.getOutput());
                    assertEquals("ok", ruleContext.getMessage());
                    assertEquals(1, ruleContext.getFiredRules());
                });
    }

    @Test
    void appliesCompilerPropertiesFromSpringEnvironmentBeforeBootstrap() {
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            resetInitializedFlag();
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(CoredeuxAutoConfiguration.class,
                            CoredeuxDrlAutoConfiguration.class))
                    .withBean(CoredeuxRequestContextResolver.class, () -> () -> null)
                    .withUserConfiguration(SampleServiceConfiguration.class)
                    .withPropertyValues(
                            "coredeux.drl.classpath-prefix=custom/",
                            "coredeux.drl.classpath-suffix=.rule",
                            "coredeux.drl.java-compiler=ECJ",
                            "coredeux.drl.java-language-level=17")
                    .run(context -> {
                        assertEquals("ECJ", System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY));
                        assertEquals("17", System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY));
                        assertInstanceOf(SpringCoredeuxComponentRegistry.class,
                                context.getBean(CoredeuxComponentRegistry.class));
                    });
        } finally {
            restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, previousCompiler);
            restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, previousLanguageLevel);
            resetInitializedFlag();
        }
    }

    @Test
    void ignoresBlankCompilerPropertiesAndFallsBackToDefaults() {
        String previousConfigCompiler = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY);
        String previousConfigLanguageLevel = System.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY);
        String previousCompiler = System.getProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY);
        String previousLanguageLevel = System.getProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY);
        try {
            resetInitializedFlag();
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, null);
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, null);
            restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, null);
            restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, null);
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("coredeux.drl.classpath-prefix", " ")
                    .withProperty("coredeux.drl.classpath-suffix", " ")
                    .withProperty("coredeux.drl.java-compiler", " ")
                    .withProperty("coredeux.drl.java-language-level", " ");

            CoredeuxDrlAutoConfiguration.DrlCompilerPropertiesConfigurer configurer =
                    configuration.coredeuxDrlCompilerPropertiesConfigurer(environment);
            DRLSourceResolver resolver = configuration.coredeuxDrlSourceResolver(environment);

            assertEquals("NATIVE", DrlRuntimeBootstrap.resolveJavaCompiler());
            assertEquals(expectedLanguageLevelForCurrentRuntime(), DrlRuntimeBootstrap.resolveJavaLanguageLevel());
            assertTrue(resolver.resolve("sample-rule")
                    .contains("global com.coredeux.core.registry.CoredeuxComponentRegistry componentRegistry;"));
        } finally {
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY, previousConfigCompiler);
            restoreProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY, previousConfigLanguageLevel);
            restoreProperty(DrlRuntimeBootstrap.JAVA_COMPILER_PROPERTY, previousCompiler);
            restoreProperty(DrlRuntimeBootstrap.JAVA_LANGUAGE_LEVEL_PROPERTY, previousLanguageLevel);
            resetInitializedFlag();
        }
    }

    private void restoreProperty(String name, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previousValue);
        }
    }

    private void resetInitializedFlag() {
        try {
            Field field = DrlRuntimeBootstrap.class.getDeclaredField("INITIALIZED");
            field.setAccessible(true);
            AtomicBoolean initialized = (AtomicBoolean) field.get(null);
            initialized.set(false);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to reset DRL bootstrap state for the test", exception);
        }
    }

    private String expectedLanguageLevelForCurrentRuntime() {
        return Runtime.version().feature() >= 19 ? "19" : "17";
    }

    @Configuration(proxyBeanMethods = false)
    static class SampleServiceConfiguration {

        @Bean
        SampleService sampleService() {
            return new SampleService();
        }
    }
}
