package com.coredeux.spring.boot.autoconfigure;

import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.export.handler.CoredeuxExportValueHandler;
import com.coredeux.export.handler.ExportValueHandlerResolver;
import com.coredeux.export.handler.impl.DefaultCoredeuxExportValueHandler;
import com.coredeux.export.log.CoredeuxExportLogService;
import com.coredeux.export.log.CoredeuxExportLogServiceResolver;
import com.coredeux.export.log.impl.ConsoleCoredeuxExportLogService;
import com.coredeux.export.log.impl.DefaultCoredeuxExportLogServiceResolver;
import com.coredeux.export.log.impl.FileCoredeuxExportLogService;
import com.coredeux.export.queue.CoredeuxExportQueueService;
import com.coredeux.export.queue.impl.FileCoredeuxExportQueueService;
import com.coredeux.export.service.CoredeuxExportExecutionService;
import com.coredeux.export.service.CoredeuxExportService;
import com.coredeux.export.service.impl.DefaultCoredeuxExportService;
import com.coredeux.export.service.impl.ExportFieldPathParser;
import com.coredeux.export.service.impl.ExportValueFormatter;
import com.coredeux.export.service.impl.ExportValueResolver;
import com.coredeux.export.storage.CoredeuxExportStorageService;
import com.coredeux.export.storage.CoredeuxExportStorageServiceResolver;
import com.coredeux.export.storage.impl.DefaultCoredeuxExportStorageServiceResolver;
import com.coredeux.export.storage.impl.DefaultCoredeuxFileSystemExportStorageService;
import com.coredeux.export.worker.DefaultCoredeuxExportWorker;
import com.coredeux.export.writer.ExcelExportWriter;
import com.coredeux.export.writer.TextExportWriter;

@AutoConfiguration(after = CoredeuxAutoConfiguration.class)
@ConditionalOnClass(CoredeuxExportService.class)
@EnableScheduling
public class CoredeuxExportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CoredeuxExportProperties coredeuxExportProperties(CoredeuxProperties coredeuxProperties,
            Environment environment) {
        return new CoredeuxExportProperties(coredeuxProperties, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    ExportFieldPathParser coredeuxExportFieldPathParser() {
        return new ExportFieldPathParser();
    }

    @Bean
    @ConditionalOnMissingBean
    ExportValueFormatter coredeuxExportValueFormatter() {
        return new ExportValueFormatter();
    }

    @Bean(name = ExportValueHandlerResolver.DEFAULT_HANDLER)
    @ConditionalOnMissingBean(name = ExportValueHandlerResolver.DEFAULT_HANDLER)
    CoredeuxExportValueHandler coredeuxDefaultExportValueHandler() {
        return new DefaultCoredeuxExportValueHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    ExportValueHandlerResolver coredeuxExportValueHandlerResolver(
            Map<String, CoredeuxExportValueHandler> handlers) {
        return new ExportValueHandlerResolver(handlers);
    }

    @Bean
    @ConditionalOnMissingBean
    ExportValueResolver coredeuxExportValueResolver(CoredeuxReflectionHelperService reflectionHelperService,
            ExportValueFormatter formatter, ExportValueHandlerResolver handlerResolver) {
        return new ExportValueResolver(reflectionHelperService, formatter, handlerResolver);
    }

    @Bean(name = "consoleCoredeuxExportLogService")
    @ConditionalOnMissingBean(name = "consoleCoredeuxExportLogService")
    CoredeuxExportLogService consoleCoredeuxExportLogService() {
        return new ConsoleCoredeuxExportLogService();
    }

    @Bean(name = DefaultCoredeuxExportLogServiceResolver.DEFAULT_LOG_SERVICE)
    @ConditionalOnMissingBean(name = DefaultCoredeuxExportLogServiceResolver.DEFAULT_LOG_SERVICE)
    CoredeuxExportLogService coredeuxDefaultExportLogService(CoredeuxExportProperties properties) {
        String baseDirectory = properties.string("log.base-directory", FileCoredeuxExportLogService.DEFAULT_BASE_DIRECTORY);
        return new FileCoredeuxExportLogService(baseDirectory);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxExportLogServiceResolver coredeuxExportLogServiceResolver(Map<String, CoredeuxExportLogService> services,
            CoredeuxExportProperties properties) {
        String defaultLogService = properties.string("log.default-service",
                DefaultCoredeuxExportLogServiceResolver.DEFAULT_LOG_SERVICE);
        return new DefaultCoredeuxExportLogServiceResolver(services, defaultLogService);
    }

    @Bean(name = FileCoredeuxExportQueueService.DEFAULT_QUEUE_SERVICE)
    @ConditionalOnMissingBean(name = FileCoredeuxExportQueueService.DEFAULT_QUEUE_SERVICE)
    CoredeuxExportQueueService coredeuxExportQueueService(CoredeuxExportProperties properties) {
        String baseDirectory = properties.string("queue.base-directory", FileCoredeuxExportQueueService.DEFAULT_BASE_DIRECTORY);
        return new FileCoredeuxExportQueueService(baseDirectory);
    }

    @Bean(name = DefaultCoredeuxExportStorageServiceResolver.DEFAULT_STORAGE_SERVICE)
    @ConditionalOnMissingBean(name = DefaultCoredeuxExportStorageServiceResolver.DEFAULT_STORAGE_SERVICE)
    CoredeuxExportStorageService coredeuxDefaultExportStorageService(CoredeuxExportProperties properties) {
        String baseDirectory = properties.string("storage.filesystem.base-directory",
                DefaultCoredeuxFileSystemExportStorageService.DEFAULT_BASE_DIRECTORY);
        return new DefaultCoredeuxFileSystemExportStorageService(baseDirectory);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxExportStorageServiceResolver coredeuxExportStorageServiceResolver(
            Map<String, CoredeuxExportStorageService> services, CoredeuxExportProperties properties) {
        String defaultStorageService = properties.string("storage.default-service",
                DefaultCoredeuxExportStorageServiceResolver.DEFAULT_STORAGE_SERVICE);
        return new DefaultCoredeuxExportStorageServiceResolver(services, defaultStorageService);
    }

    @Bean
    @ConditionalOnMissingBean
    TextExportWriter coredeuxTextExportWriter() {
        return new TextExportWriter();
    }

    @Bean
    @ConditionalOnMissingBean
    ExcelExportWriter coredeuxExcelExportWriter() {
        return new ExcelExportWriter();
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxExportService coredeuxExportService(CoredeuxService coredeuxService,
            CoredeuxReflectionHelperService reflectionHelperService,
            EntityDefinitionRegistry entityDefinitionRegistry,
            ExportFieldPathParser fieldPathParser, ExportValueResolver valueResolver,
            CoredeuxExportStorageServiceResolver storageServiceResolver,
            CoredeuxExportLogServiceResolver logServiceResolver, CoredeuxExportQueueService queueService,
            TextExportWriter textWriter, ExcelExportWriter excelWriter, CoredeuxExportProperties properties) {
        return new DefaultCoredeuxExportService(coredeuxService, reflectionHelperService, entityDefinitionRegistry,
                fieldPathParser, valueResolver, storageServiceResolver, logServiceResolver, queueService, textWriter,
                excelWriter, properties.defaultFormat());
    }

    @Bean
    @ConditionalOnMissingBean
    DefaultCoredeuxExportWorker coredeuxExportWorker(CoredeuxExportQueueService queueService,
            CoredeuxExportExecutionService executionService, CoredeuxExportLogServiceResolver logServiceResolver,
            CoredeuxExportProperties properties) {
        int maxParallel = properties.integer("worker.max-parallel", 2);
        boolean enabled = properties.booleanValue("worker.enabled", true);
        return new DefaultCoredeuxExportWorker(queueService, executionService, logServiceResolver, maxParallel,
                enabled);
    }

    @Bean
    @ConditionalOnMissingBean
    CoredeuxExportWorkerScheduler coredeuxExportWorkerScheduler(DefaultCoredeuxExportWorker worker) {
        return new CoredeuxExportWorkerScheduler(worker);
    }
}
