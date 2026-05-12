package com.coredeux.spring.boot.autoconfigure;

import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.impex.handler.CoredeuxImportValueHandler;
import com.coredeux.impex.handler.ImportValueHandlerResolver;
import com.coredeux.impex.handler.impl.DefaultCoredeuxImportValueHandler;
import com.coredeux.impex.handler.impl.JsonMapImportHandler;
import com.coredeux.impex.parser.excel.CoredeuxExcelImportParser;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;
import com.coredeux.impex.service.CoredeuxImportService;
import com.coredeux.impex.service.impl.DefaultCoredeuxImportService;
import com.coredeux.impex.service.impl.ImportEntityTargetService;
import org.apache.poi.EncryptedDocumentException;

@AutoConfiguration(after = CoredeuxAutoConfiguration.class)
@ConditionalOnClass(CoredeuxImportService.class)
public class CoredeuxImportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CoredeuxImportProperties coredeuxImportProperties(CoredeuxProperties coredeuxProperties,
            Environment environment) {
        return new CoredeuxImportProperties(coredeuxProperties, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxTextImportParser coredeuxTextImportParser() {
        return new CoredeuxTextImportParser();
    }

    @Bean
    @ConditionalOnClass(EncryptedDocumentException.class)
    @ConditionalOnMissingBean
    CoredeuxExcelImportParser coredeuxExcelImportParser() {
        return new CoredeuxExcelImportParser();
    }

    @Bean(name = ImportValueHandlerResolver.DEFAULT_HANDLER)
    @ConditionalOnMissingBean(name = ImportValueHandlerResolver.DEFAULT_HANDLER)
    CoredeuxImportValueHandler coredeuxDefaultImportValueHandler(CoredeuxService coredeuxService) {
        return new DefaultCoredeuxImportValueHandler(coredeuxService);
    }

    @Bean(name = "jsonMapImportHandler")
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    @ConditionalOnMissingBean(name = "jsonMapImportHandler")
    CoredeuxImportValueHandler jsonMapImportHandler() {
        return new JsonMapImportHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    ImportValueHandlerResolver coredeuxImportValueHandlerResolver(
            Map<String, CoredeuxImportValueHandler> handlers) {
        return new ImportValueHandlerResolver(handlers);
    }

    @Bean
    @ConditionalOnMissingBean
    ImportEntityTargetService coredeuxImportEntityTargetService(
            CoredeuxReflectionHelperService reflectionHelperService,
            EntityDefinitionRegistry entityDefinitionRegistry) {
        return new ImportEntityTargetService(reflectionHelperService, entityDefinitionRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxImportService coredeuxImportService(CoredeuxService coredeuxService,
            CoredeuxReflectionHelperService reflectionHelperService,
            ImportEntityTargetService entityTargetService,
            ImportValueHandlerResolver valueHandlerResolver) {
        return new DefaultCoredeuxImportService(coredeuxService, reflectionHelperService, entityTargetService,
                valueHandlerResolver);
    }
}
