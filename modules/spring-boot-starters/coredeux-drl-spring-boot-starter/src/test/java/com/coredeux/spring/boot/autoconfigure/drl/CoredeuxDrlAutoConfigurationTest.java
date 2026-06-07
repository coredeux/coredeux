package com.coredeux.spring.boot.autoconfigure.drl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;
import com.coredeux.spring.boot.autoconfigure.CoredeuxAutoConfiguration;
import com.coredeux.spring.boot.autoconfigure.SpringCoredeuxComponentRegistry;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class CoredeuxDrlAutoConfigurationTest {

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
                        end
                        """)
                .run(context -> {
                    DRLService drlService = context.getBean(DRLService.class);
                    RuleContext ruleContext = RuleContext.method("sample-rule");
                    drlService.execute("sample-rule", ruleContext);

                    assertEquals("ok", ruleContext.getOutput());
                    assertEquals(1, ruleContext.getFiredRules());
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class SampleServiceConfiguration {

        @Bean
        SampleService sampleService() {
            return new SampleService();
        }
    }
}
