package com.coredeux.examples.nativejava;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.postgresql.ds.PGSimpleDataSource;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.config.CoredeuxPropertiesLoader;
import com.coredeux.core.elasticsearch.service.impl.DefaultCoredeuxElasticsearchDataAccessService;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.jdbc.service.impl.DefaultCoredeuxJdbcDataAccessService;
import com.coredeux.core.jpa.service.impl.PostgresCoredeuxJpaDataAccessService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.module.impl.AuditModuleHandler;
import com.coredeux.core.module.impl.HooksModuleHandler;
import com.coredeux.core.module.impl.ValidatorsModuleHandler;
import com.coredeux.core.mongodb.service.impl.DefaultCoredeuxMongoDataAccessService;
import com.coredeux.core.redis.service.impl.DefaultCoredeuxRedisDataAccessService;
import com.coredeux.core.registry.EntityDefinitionRegistries;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.core.resolver.EntityDefinitionBackedDataAccessResolver;
import com.coredeux.core.service.CoredeuxModuleService;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.core.service.impl.DefaultCoredeuxModuleService;
import com.coredeux.core.service.impl.DefaultCoredeuxService;
import com.coredeux.core.strategy.CoredeuxStrategy;
import com.coredeux.core.strategy.impl.DefaultCoredeuxStrategy;
import com.coredeux.demo.audit.DemoAuditHandler;
import com.coredeux.demo.hooks.DemoLifecycleHook;
import com.coredeux.demo.validation.CustomerEmailValidator;
import com.coredeux.demo.workflow.DemoCustomerApprovalWorkflow;
import com.coredeux.demo.workflow.WorkflowsModuleHandler;
import com.coredeux.drl.service.DRLService;
import com.coredeux.drl.service.impl.DefaultDRLService;
import com.coredeux.export.handler.ExportValueHandlerResolver;
import com.coredeux.export.handler.impl.DefaultCoredeuxExportValueHandler;
import com.coredeux.export.log.CoredeuxExportLogServiceResolver;
import com.coredeux.export.log.impl.DefaultCoredeuxExportLogServiceResolver;
import com.coredeux.export.log.impl.FileCoredeuxExportLogService;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.queue.CoredeuxExportQueueService;
import com.coredeux.export.queue.impl.FileCoredeuxExportQueueService;
import com.coredeux.export.service.CoredeuxExportExecutionService;
import com.coredeux.export.service.CoredeuxExportService;
import com.coredeux.export.service.impl.DefaultCoredeuxExportService;
import com.coredeux.export.service.impl.ExportFieldPathParser;
import com.coredeux.export.service.impl.ExportValueFormatter;
import com.coredeux.export.service.impl.ExportValueResolver;
import com.coredeux.export.storage.CoredeuxExportStorageServiceResolver;
import com.coredeux.export.storage.impl.DefaultCoredeuxExportStorageServiceResolver;
import com.coredeux.export.storage.impl.DefaultCoredeuxFileSystemExportStorageService;
import com.coredeux.export.worker.DefaultCoredeuxExportWorker;
import com.coredeux.export.writer.ExcelExportWriter;
import com.coredeux.export.writer.TextExportWriter;
import com.coredeux.impex.handler.ImportValueHandlerResolver;
import com.coredeux.impex.handler.impl.DefaultCoredeuxImportValueHandler;
import com.coredeux.impex.handler.impl.JsonMapImportHandler;
import com.coredeux.impex.service.CoredeuxImportService;
import com.coredeux.impex.service.impl.DefaultCoredeuxImportService;
import com.coredeux.impex.service.impl.ImportEntityTargetService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import com.coredeux.examples.nativejava.drl.DemoGreetingService;
import com.coredeux.examples.nativejava.drl.NativeDrlRuleSourceService;

public final class CoredeuxNativeRuntime implements AutoCloseable {

    private final EntityDefinitionRegistry entityDefinitionRegistry;
    private final CoredeuxProperties coredeuxProperties;
    private final PostgresCoredeuxJpaDataAccessService customerDataAccessService;
    private final CoredeuxService coredeuxService;
    private final CoredeuxModuleService coredeuxModuleService;
    private final CoredeuxImportService coredeuxImportService;
    private final CoredeuxExportService coredeuxExportService;
    private final NativeDrlRuleSourceService drlRuleSourceService;
    private final DRLService drlService;
    private final DefaultCoredeuxExportWorker coredeuxExportWorker;
    private final Thread exportWorkerSchedulerThread;
    private final CoredeuxReflectionHelperService reflectionHelperService;

    private CoredeuxNativeRuntime(CoredeuxProperties coredeuxProperties,
            EntityDefinitionRegistry entityDefinitionRegistry,
            PostgresCoredeuxJpaDataAccessService customerDataAccessService,
            CoredeuxService coredeuxService,
            CoredeuxModuleService coredeuxModuleService,
            CoredeuxImportService coredeuxImportService,
            CoredeuxExportService coredeuxExportService,
            NativeDrlRuleSourceService drlRuleSourceService,
            DRLService drlService,
            DefaultCoredeuxExportWorker coredeuxExportWorker,
            Thread exportWorkerSchedulerThread,
            CoredeuxReflectionHelperService reflectionHelperService) {
        this.coredeuxProperties = coredeuxProperties;
        this.entityDefinitionRegistry = entityDefinitionRegistry;
        this.customerDataAccessService = customerDataAccessService;
        this.coredeuxService = coredeuxService;
        this.coredeuxModuleService = coredeuxModuleService;
        this.coredeuxImportService = coredeuxImportService;
        this.coredeuxExportService = coredeuxExportService;
        this.drlRuleSourceService = drlRuleSourceService;
        this.drlService = drlService;
        this.coredeuxExportWorker = coredeuxExportWorker;
        this.exportWorkerSchedulerThread = exportWorkerSchedulerThread;
        this.reflectionHelperService = reflectionHelperService;
    }

    public static CoredeuxNativeRuntime create() {
        CoredeuxProperties coredeuxProperties = new CoredeuxPropertiesLoader().load();

        EntityDefinitionRegistry registry;
        String entityConfigLocation = coredeuxProperties.string("entities.config-location");
        if (entityConfigLocation == null || entityConfigLocation.isBlank()) {
            entityConfigLocation = "classpath:coredeux-entities.yml";
        }

        try (InputStream inputStream = openStream(entityConfigLocation)) {
            registry = EntityDefinitionRegistries.fromYaml(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load Coredeux entity definitions", exception);
        }

        Map<String, String> postgresProperties = postgresProperties(coredeuxProperties);
        EntityManagerFactory entityManagerFactory = postgresProperties.isEmpty()
                ? Persistence.createEntityManagerFactory("coredeux-postgres-native")
                : Persistence.createEntityManagerFactory("coredeux-postgres-native", postgresProperties);

        PostgresCoredeuxJpaDataAccessService customerDataAccess =
                new PostgresCoredeuxJpaDataAccessService(entityManagerFactory);
        DefaultCoredeuxJdbcDataAccessService jdbcDataAccessService =
                new DefaultCoredeuxJdbcDataAccessService(postgresDataSource(coredeuxProperties), "");
        MongoClient mongoClient = mongoClient(coredeuxProperties);
        DefaultCoredeuxMongoDataAccessService mongoDataAccessService =
                new DefaultCoredeuxMongoDataAccessService(mongoClient, mongoDatabaseName(coredeuxProperties));
        RestClient elasticsearchRestClient = elasticsearchRestClient(coredeuxProperties);
        ElasticsearchClient elasticsearchClient = new ElasticsearchClient(
                new RestClientTransport(elasticsearchRestClient, new JacksonJsonpMapper()));
        DefaultCoredeuxElasticsearchDataAccessService elasticsearchDataAccessService =
                new DefaultCoredeuxElasticsearchDataAccessService(elasticsearchClient, "");
        RedisClient redisClient = redisClient(coredeuxProperties);
        StatefulRedisConnection<String, String> redisConnection = redisClient.connect(StringCodec.UTF8);
        DefaultCoredeuxRedisDataAccessService redisDataAccessService =
                new DefaultCoredeuxRedisDataAccessService(redisConnection, "");

        InMemoryCoredeuxComponentRegistry components = InMemoryCoredeuxComponentRegistry.builder()
                .component("postgresCustomerDataAccess", customerDataAccess)
                .component("defaultCoredeuxJdbcDataAccessService", jdbcDataAccessService)
                .component("defaultCoredeuxMongoDataAccessService", mongoDataAccessService)
                .component("defaultCoredeuxElasticsearchDataAccessService", elasticsearchDataAccessService)
                .component("defaultCoredeuxRedisDataAccessService", redisDataAccessService)
                .component("customerEmailValidator", new CustomerEmailValidator())
                .component("demoLifecycleHook", new DemoLifecycleHook())
                .component("demoAuditHandler", new DemoAuditHandler())
                .component("customerApprovalWorkflow", new DemoCustomerApprovalWorkflow())
                .component("demoGreetingService", new DemoGreetingService())
                .component("demoUriImportHandler", new com.coredeux.demo.imports.DemoUriImportHandler())
                .component("legacyDateImportHandler", new com.coredeux.demo.imports.LegacyDateImportHandler())
                .component("dateFormatExportHandler", new com.coredeux.demo.export.DateFormatExportHandler())
                .component("exportStorageCleanupHook", new com.coredeux.demo.hooks.ExportStorageCleanupHook(new com.fasterxml.jackson.databind.ObjectMapper()))
                .build();

        NativeDrlRuleSourceService drlRuleSourceService = new NativeDrlRuleSourceService(entityManagerFactory);
        drlRuleSourceService.seedSampleRule();
        DRLService drlService = new DefaultDRLService(drlRuleSourceService, components);

        CoredeuxReflectionHelperService reflectionHelperService = new DefaultCoredeuxReflectionHelperService();
        List<CoredeuxEntityModuleHandler> moduleHandlers = List.of(
                new ValidatorsModuleHandler(components),
                new HooksModuleHandler(components),
                new AuditModuleHandler(components),
                new WorkflowsModuleHandler(components));

        CoredeuxStrategy strategy = new DefaultCoredeuxStrategy(registry,
                new EntityDefinitionBackedDataAccessResolver(),
                components,
                reflectionHelperService,
                () -> null,
                moduleHandlers);

        CoredeuxModuleService moduleService = new DefaultCoredeuxModuleService(registry,
                new EntityDefinitionBackedDataAccessResolver(),
                components,
                reflectionHelperService,
                () -> null,
                moduleHandlers);

        CoredeuxService coredeuxService = new DefaultCoredeuxService(strategy);
        CoredeuxImportService coredeuxImportService = importService(coredeuxService, registry, reflectionHelperService);
        CoredeuxExportExecutionService coredeuxExportExecutionService = exportService(coredeuxProperties,
                coredeuxService, registry, reflectionHelperService);
        CoredeuxExportService coredeuxExportService = (CoredeuxExportService) coredeuxExportExecutionService;
        DefaultCoredeuxExportWorker exportWorker = exportWorker(coredeuxProperties,
                coredeuxExportExecutionService);
        Thread exportWorkerSchedulerThread = scheduleExportWorker(coredeuxProperties, exportWorker);

        return new CoredeuxNativeRuntime(coredeuxProperties, registry, customerDataAccess, coredeuxService,
                moduleService, coredeuxImportService, coredeuxExportService, drlRuleSourceService, drlService, exportWorker,
                exportWorkerSchedulerThread, reflectionHelperService);
    }

    public CoredeuxProperties coredeuxProperties() {
        return coredeuxProperties;
    }

    public EntityDefinitionRegistry entityDefinitionRegistry() {
        return entityDefinitionRegistry;
    }

    public PostgresCoredeuxJpaDataAccessService customerDataAccessService() {
        return customerDataAccessService;
    }

    public CoredeuxService coredeuxService() {
        return coredeuxService;
    }

    public CoredeuxModuleService coredeuxModuleService() {
        return coredeuxModuleService;
    }

    public CoredeuxReflectionHelperService reflectionHelperService() {
        return reflectionHelperService;
    }

    public CoredeuxImportService coredeuxImportService() {
        return coredeuxImportService;
    }

    public CoredeuxExportService coredeuxExportService() {
        return coredeuxExportService;
    }

    public NativeDrlRuleSourceService drlRuleSourceService() {
        return drlRuleSourceService;
    }

    public DRLService drlService() {
        return drlService;
    }

    public DefaultCoredeuxExportWorker coredeuxExportWorker() {
        return coredeuxExportWorker;
    }

    public void processPendingExportJobs() {
        coredeuxExportWorker.processPendingExports();
    }

    @Override
    public void close() {
        try {
            if (exportWorkerSchedulerThread != null) {
                exportWorkerSchedulerThread.interrupt();
            }
            if (coredeuxExportWorker != null) {
                coredeuxExportWorker.shutdown();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            customerDataAccessService.close();
        }
    }

    public static String customerId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static CoredeuxImportService importService(CoredeuxService coredeuxService,
            EntityDefinitionRegistry registry, CoredeuxReflectionHelperService reflectionHelperService) {
        ImportEntityTargetService targetService = new ImportEntityTargetService(reflectionHelperService, registry);
        ImportValueHandlerResolver valueHandlerResolver = new ImportValueHandlerResolver(Map.of(
                ImportValueHandlerResolver.DEFAULT_HANDLER, new DefaultCoredeuxImportValueHandler(coredeuxService),
                "jsonMapImportHandler", new JsonMapImportHandler()));
        return new DefaultCoredeuxImportService(coredeuxService, reflectionHelperService, targetService,
                valueHandlerResolver);
    }

    private static CoredeuxExportExecutionService exportService(CoredeuxProperties coredeuxProperties,
            CoredeuxService coredeuxService, EntityDefinitionRegistry registry,
            CoredeuxReflectionHelperService reflectionHelperService) {
        String defaultFormatValue = coredeuxProperties.string("export.default-format");
        ExportFormat defaultFormat = exportFormat(defaultFormatValue);

        CoredeuxExportStorageServiceResolver storageResolver = new DefaultCoredeuxExportStorageServiceResolver(Map.of(
                DefaultCoredeuxExportStorageServiceResolver.DEFAULT_STORAGE_SERVICE,
                new DefaultCoredeuxFileSystemExportStorageService(exportBaseDirectory(coredeuxProperties))));
        CoredeuxExportLogServiceResolver logResolver = new DefaultCoredeuxExportLogServiceResolver(Map.of(
                DefaultCoredeuxExportLogServiceResolver.DEFAULT_LOG_SERVICE,
                new FileCoredeuxExportLogService(exportLogDirectory(coredeuxProperties))));
        CoredeuxExportQueueService queueService = new FileCoredeuxExportQueueService(exportQueueDirectory(coredeuxProperties));
        ExportValueHandlerResolver handlerResolver = new ExportValueHandlerResolver(Map.of(
                ExportValueHandlerResolver.DEFAULT_HANDLER, new DefaultCoredeuxExportValueHandler()));
        ExportValueResolver valueResolver = new ExportValueResolver(reflectionHelperService, new ExportValueFormatter(),
                handlerResolver);
        return new DefaultCoredeuxExportService(coredeuxService, reflectionHelperService, registry,
                new ExportFieldPathParser(), valueResolver, storageResolver, logResolver, queueService,
                new TextExportWriter(), new ExcelExportWriter(), defaultFormat, exportDirectory(coredeuxProperties));
    }

    private static DefaultCoredeuxExportWorker exportWorker(CoredeuxProperties coredeuxProperties,
            CoredeuxExportExecutionService exportExecutionService) {
        CoredeuxExportQueueService queueService = new FileCoredeuxExportQueueService(exportQueueDirectory(coredeuxProperties));
        CoredeuxExportLogServiceResolver logResolver = new DefaultCoredeuxExportLogServiceResolver(Map.of(
                DefaultCoredeuxExportLogServiceResolver.DEFAULT_LOG_SERVICE,
                new FileCoredeuxExportLogService(exportLogDirectory(coredeuxProperties))));
        boolean enabled = booleanValue(coredeuxProperties.string("export.worker.enabled"), true);
        int maxParallel = intValue(coredeuxProperties.string("export.worker.max-parallel"), 2);
        return new DefaultCoredeuxExportWorker(queueService, exportExecutionService,
                logResolver, maxParallel, enabled);
    }

    private static Thread scheduleExportWorker(CoredeuxProperties coredeuxProperties,
            DefaultCoredeuxExportWorker exportWorker) {
        if (exportWorker == null || !booleanValue(coredeuxProperties.string("export.worker.enabled"), true)) {
            return null;
        }
        long delay = longValue(coredeuxProperties.string("export.worker.delay-ms"), 5000L);
        Thread scheduler = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    exportWorker.processPendingExports();
                    Thread.sleep(Math.max(100L, delay));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException ignored) {
                }
            }
        }, "coredeux-export-worker");
        scheduler.setDaemon(true);
        scheduler.start();
        return scheduler;
    }

    private static ExportFormat exportFormat(String value) {
        if (value == null || value.isBlank()) {
            return ExportFormat.TEXT;
        }
        try {
            return ExportFormat.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ExportFormat.TEXT;
        }
    }

    private static String exportBaseDirectory(CoredeuxProperties coredeuxProperties) {
        String configured = coredeuxProperties.string("export.storage.filesystem.base-directory");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return Path.of(System.getProperty("java.io.tmpdir"), "coredeux-native-export").toString();
    }

    private static String exportDirectory(CoredeuxProperties coredeuxProperties) {
        String configured = coredeuxProperties.string("export.directory");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return null;
    }

    private static String exportQueueDirectory(CoredeuxProperties coredeuxProperties) {
        return Path.of(exportBaseDirectory(coredeuxProperties), "queue").toString();
    }

    private static String exportLogDirectory(CoredeuxProperties coredeuxProperties) {
        return Path.of(exportBaseDirectory(coredeuxProperties), "logs").toString();
    }

    private static PGSimpleDataSource postgresDataSource(CoredeuxProperties coredeuxProperties) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[] { stringValue(coredeuxProperties.string("demo.database.host"), "localhost") });
        dataSource.setPortNumbers(new int[] { intValue(coredeuxProperties.string("demo.database.port"), 5432) });
        dataSource.setDatabaseName(stringValue(coredeuxProperties.string("demo.database.name"), "coredeux_oss"));
        dataSource.setUser(stringValue(coredeuxProperties.string("demo.database.user"), "postgres"));
        dataSource.setPassword(stringValue(coredeuxProperties.string("demo.database.password"), "root"));
        return dataSource;
    }

    private static MongoClient mongoClient(CoredeuxProperties coredeuxProperties) {
        String host = value("COREDEUX_DEMO_MONGO_HOST", coredeuxProperties.string("demo.mongodb.host"),
                "localhost");
        int port = intValue(value("COREDEUX_DEMO_MONGO_PORT", coredeuxProperties.string("demo.mongodb.port"),
                "27017"), 27017);
        return MongoClients.create("mongodb://" + host + ":" + port);
    }

    private static String mongoDatabaseName(CoredeuxProperties coredeuxProperties) {
        return stringValue(coredeuxProperties.string("demo.mongodb.database"), "coredeux_oss");
    }

    private static RestClient elasticsearchRestClient(CoredeuxProperties coredeuxProperties) {
        String host = value("COREDEUX_DEMO_ELASTICSEARCH_HOST", coredeuxProperties.string("demo.elasticsearch.host"),
                "localhost");
        int port = intValue(value("COREDEUX_DEMO_ELASTICSEARCH_PORT",
                coredeuxProperties.string("demo.elasticsearch.port"), "9200"), 9200);
        String scheme = value("COREDEUX_DEMO_ELASTICSEARCH_SCHEME",
                coredeuxProperties.string("demo.elasticsearch.scheme"), "http");
        return RestClient.builder(new HttpHost(host, port, scheme)).build();
    }

    private static RedisClient redisClient(CoredeuxProperties coredeuxProperties) {
        String host = value("COREDEUX_DEMO_REDIS_HOST", coredeuxProperties.string("demo.redis.host"), "localhost");
        int port = intValue(value("COREDEUX_DEMO_REDIS_PORT", coredeuxProperties.string("demo.redis.port"), "6379"),
                6379);
        return RedisClient.create(RedisURI.Builder.redis(host, port).build());
    }

    private static int intValue(String value, int defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static String stringValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static long longValue(String value, long defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static boolean booleanValue(String value, boolean defaultValue) {
        return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private static Map<String, String> postgresProperties(CoredeuxProperties coredeuxProperties) {
        Map<String, String> properties = new HashMap<>();
        putValue(properties, "jakarta.persistence.jdbc.url", jdbcUrl(coredeuxProperties));
        putValue(properties, "jakarta.persistence.jdbc.user",
                value("COREDEUX_POSTGRES_USER", coredeuxProperties.string("demo.database.user"), "postgres"));
        putValue(properties, "jakarta.persistence.jdbc.password",
                value("COREDEUX_POSTGRES_PASSWORD", coredeuxProperties.string("demo.database.password"), "root"));
        return properties;
    }

    private static String jdbcUrl(CoredeuxProperties coredeuxProperties) {
        String url = env("COREDEUX_POSTGRES_URL");
        if (url != null) {
            return url;
        }
        String host = value("COREDEUX_POSTGRES_HOST", coredeuxProperties.string("demo.database.host"), "localhost");
        int port = intValue(value("COREDEUX_POSTGRES_PORT", coredeuxProperties.string("demo.database.port"), "5432"),
                5432);
        String database = value("COREDEUX_POSTGRES_DB", coredeuxProperties.string("demo.database.name"),
                "coredeux_oss");
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private static void putValue(Map<String, String> properties, String propertyName, String value) {
        if (value != null && !value.isBlank()) {
            properties.put(propertyName, value);
        }
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
    }

    private static String value(String envName, String configuredValue, String defaultValue) {
        String envValue = env(envName);
        if (envValue != null) {
            return envValue;
        }
        return configuredValue == null || configuredValue.isBlank() ? defaultValue : configuredValue.trim();
    }

    private static InputStream openStream(String location) throws IOException {
        String normalized = location == null ? "" : location.trim();
        if (normalized.startsWith("classpath:")) {
            String resourceName = normalized.substring("classpath:".length());
            if (resourceName.startsWith("/")) {
                resourceName = resourceName.substring(1);
            }
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = CoredeuxNativeRuntime.class.getClassLoader();
            }
            InputStream inputStream = classLoader.getResourceAsStream(resourceName);
            if (inputStream == null) {
                throw new IOException("Missing classpath resource: " + resourceName);
            }
            return inputStream;
        }

        return Files.newInputStream(Path.of(normalized));
    }
}
