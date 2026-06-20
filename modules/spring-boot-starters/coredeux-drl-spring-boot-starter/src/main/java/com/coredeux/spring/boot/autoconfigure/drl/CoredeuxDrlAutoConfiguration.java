package com.coredeux.spring.boot.autoconfigure.drl;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.handler.service.CoredeuxValueHandlerService;
import com.coredeux.core.module.impl.AuditModuleHandler;
import com.coredeux.core.module.impl.HooksModuleHandler;
import com.coredeux.core.module.impl.ValidatorsModuleHandler;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.resolver.EntityDataAccessResolver;
import com.coredeux.core.resolver.CoredeuxEntityDefinitionResolver;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;
import com.coredeux.core.strategy.CoredeuxStrategy;
import com.coredeux.drl.core.handler.service.DefaultDRLCoredeuxValueHandlerService;
import com.coredeux.drl.core.module.impl.DRLAuditModuleHandler;
import com.coredeux.drl.core.module.impl.DRLHooksModuleHandler;
import com.coredeux.drl.core.module.impl.DRLValidatorsModuleHandler;
import com.coredeux.drl.core.strategy.impl.DefaultDRLCoredeuxStrategy;
import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.service.DRLService;
import com.coredeux.drl.service.impl.DefaultDRLService;
import com.coredeux.drl.source.resolver.DRLSourceResolver;
import com.coredeux.drl.source.resolver.impl.ClasspathDRLSourceResolver;

@AutoConfiguration(before = com.coredeux.spring.boot.autoconfigure.CoredeuxAutoConfiguration.class)
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

    @Bean
    @ConditionalOnMissingBean
    @Primary
    CoredeuxValueHandlerService coredeuxValueHandlerService(CoredeuxComponentRegistry componentRegistry,
            DRLService drlService) {
        return new DefaultDRLCoredeuxValueHandlerService(componentRegistry, drlService);
    }

    @Bean
    @ConditionalOnMissingBean
    ValidatorsModuleHandler coredeuxDrlValidatorsModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        return new DRLValidatorsModuleHandler(componentRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    HooksModuleHandler coredeuxDrlHooksModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        return new DRLHooksModuleHandler(componentRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditModuleHandler coredeuxDrlAuditModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        return new DRLAuditModuleHandler(componentRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxStrategy coredeuxDrlStrategy(EntityDefinitionRegistry entityDefinitionRegistry,
            EntityDataAccessResolver entityDataAccessResolver, CoredeuxComponentRegistry componentRegistry,
            CoredeuxReflectionHelperService reflectionHelperService,
            CoredeuxRequestContextResolver requestContextResolver,
            CoredeuxProperties coredeuxProperties,
            CoredeuxEntityDefinitionResolver entityDefinitionResolver,
            List<CoredeuxEntityModuleHandler> moduleHandlers) {
        return new DefaultDRLCoredeuxStrategy(entityDefinitionRegistry, entityDataAccessResolver, componentRegistry,
                reflectionHelperService, requestContextResolver, coredeuxProperties, entityDefinitionResolver,
                moduleHandlers);
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
