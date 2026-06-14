package com.coredeux.spring.boot.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.config.CoredeuxPropertiesLoader;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.loader.EntityDefinitionLoader;
import com.coredeux.core.loader.YamlEntityDefinitionLoader;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.module.impl.AuditModuleHandler;
import com.coredeux.core.module.impl.HooksModuleHandler;
import com.coredeux.core.module.impl.ValidatorsModuleHandler;
import com.coredeux.core.handler.service.CoredeuxValueHandlerService;
import com.coredeux.core.handler.service.impl.DefaultCoredeuxValueHandlerService;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.registry.InMemoryEntityDefinitionRegistry;
import com.coredeux.core.resolver.EntityDataAccessResolver;
import com.coredeux.core.resolver.EntityDefinitionBackedDataAccessResolver;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;
import com.coredeux.core.service.CoredeuxModuleService;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.core.service.impl.DefaultCoredeuxModuleService;
import com.coredeux.core.service.impl.DefaultCoredeuxService;
import com.coredeux.core.snapshot.CoredeuxEntitySnapshotService;
import com.coredeux.core.snapshot.impl.DefaultCoredeuxEntitySnapshotService;
import com.coredeux.core.strategy.CoredeuxStrategy;
import com.coredeux.core.strategy.impl.DefaultCoredeuxStrategy;

@AutoConfiguration
@ConditionalOnClass(CoredeuxService.class)
public class CoredeuxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CoredeuxProperties coredeuxProperties() {
        return new CoredeuxPropertiesLoader().load();
    }

    @Bean
    @ConditionalOnMissingBean
    EntityDefinitionLoader coredeuxEntityDefinitionLoader() {
        return new YamlEntityDefinitionLoader();
    }

    @Bean
    @ConditionalOnMissingBean
    EntityDefinitionRegistry coredeuxEntityDefinitionRegistry(ResourceLoader resourceLoader, Environment environment,
            CoredeuxProperties coredeuxProperties, EntityDefinitionLoader entityDefinitionLoader) {
        String location = firstNonBlank(environment.getProperty("coredeux.entities.config-location"),
                coredeuxProperties.string("entities.config-location"), "classpath:coredeux-entities.yml");
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            return new InMemoryEntityDefinitionRegistry(entityDefinitionLoader.load(inputStream).getEntities());
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to load Coredeux entity definitions from resource: " + resource, exception);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxReflectionHelperService coredeuxReflectionHelperService() {
        return new DefaultCoredeuxReflectionHelperService();
    }

    @Bean
    @ConditionalOnMissingBean
    EntityDataAccessResolver coredeuxEntityDataAccessResolver() {
        return new EntityDefinitionBackedDataAccessResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxComponentRegistry coredeuxComponentRegistry(ApplicationContext applicationContext) {
        return new SpringCoredeuxComponentRegistry(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxValueHandlerService coredeuxValueHandlerService(CoredeuxComponentRegistry componentRegistry) {
        return new DefaultCoredeuxValueHandlerService(componentRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxEntitySnapshotService coredeuxEntitySnapshotService() {
        return new DefaultCoredeuxEntitySnapshotService();
    }

    @Bean
    @ConditionalOnMissingBean
    ValidatorsModuleHandler coredeuxValidatorsModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        return new ValidatorsModuleHandler(componentRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    HooksModuleHandler coredeuxHooksModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        return new HooksModuleHandler(componentRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditModuleHandler coredeuxAuditModuleHandler(CoredeuxComponentRegistry componentRegistry) {
        return new AuditModuleHandler(componentRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxStrategy coredeuxStrategy(EntityDefinitionRegistry entityDefinitionRegistry,
            EntityDataAccessResolver entityDataAccessResolver, CoredeuxComponentRegistry componentRegistry,
            CoredeuxReflectionHelperService reflectionHelperService,
            CoredeuxRequestContextResolver requestContextResolver,
            CoredeuxEntitySnapshotService entitySnapshotService,
            List<CoredeuxEntityModuleHandler> moduleHandlers) {
        return new DefaultCoredeuxStrategy(entityDefinitionRegistry, entityDataAccessResolver, componentRegistry,
                reflectionHelperService, requestContextResolver, entitySnapshotService, moduleHandlers);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxModuleService coredeuxModuleService(EntityDefinitionRegistry entityDefinitionRegistry,
            EntityDataAccessResolver entityDataAccessResolver, CoredeuxComponentRegistry componentRegistry,
            CoredeuxReflectionHelperService reflectionHelperService,
            CoredeuxRequestContextResolver requestContextResolver,
            CoredeuxEntitySnapshotService entitySnapshotService,
            List<CoredeuxEntityModuleHandler> moduleHandlers) {
        return new DefaultCoredeuxModuleService(entityDefinitionRegistry, entityDataAccessResolver, componentRegistry,
                reflectionHelperService, requestContextResolver, entitySnapshotService, moduleHandlers);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxService coredeuxService(CoredeuxStrategy coredeuxStrategy) {
        return new DefaultCoredeuxService(coredeuxStrategy);
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
}
