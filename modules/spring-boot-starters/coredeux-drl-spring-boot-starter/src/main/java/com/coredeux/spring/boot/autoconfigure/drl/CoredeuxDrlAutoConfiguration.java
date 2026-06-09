package com.coredeux.spring.boot.autoconfigure.drl;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.service.DRLService;
import com.coredeux.drl.service.impl.DefaultDRLService;
import com.coredeux.drl.source.resolver.DRLSourceResolver;
import com.coredeux.drl.source.resolver.impl.ClasspathDRLSourceResolver;

@AutoConfiguration(afterName = "com.coredeux.spring.boot.autoconfigure.CoredeuxAutoConfiguration")
@ConditionalOnClass(DRLService.class)
public class CoredeuxDrlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    DrlCompilerPropertiesConfigurer coredeuxDrlCompilerPropertiesConfigurer(Environment environment) {
        return new DrlCompilerPropertiesConfigurer(environment);
    }

    @Bean
    @ConditionalOnMissingBean
    DRLSourceResolver coredeuxDrlSourceResolver(Environment environment) {
        String prefix = firstNonBlank(environment.getProperty("coredeux.drl.classpath-prefix"), "rules/");
        String suffix = firstNonBlank(environment.getProperty("coredeux.drl.classpath-suffix"), ".drl");
        return new ClasspathDRLSourceResolver(Thread.currentThread().getContextClassLoader(), prefix, suffix);
    }

    @Bean
    @ConditionalOnMissingBean
    DRLService coredeuxDrlService(DRLSourceResolver sourceResolver, CoredeuxComponentRegistry componentRegistry,
            DrlCompilerPropertiesConfigurer compilerPropertiesConfigurer) {
        return new DefaultDRLService(sourceResolver, componentRegistry);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    static final class DrlCompilerPropertiesConfigurer {

        DrlCompilerPropertiesConfigurer(Environment environment) {
            apply(environment);
        }

        private void apply(Environment environment) {
            setIfPresent(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY,
                    environment.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_COMPILER_PROPERTY));
            setIfPresent(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY,
                    environment.getProperty(DrlRuntimeBootstrap.CONFIG_JAVA_LANGUAGE_LEVEL_PROPERTY));
            DrlRuntimeBootstrap.initialize();
        }

        private void setIfPresent(String propertyName, String propertyValue) {
            if (propertyValue != null && !propertyValue.isBlank()) {
                System.setProperty(propertyName, propertyValue.trim());
            }
        }
    }
}
