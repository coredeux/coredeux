package com.coredeux.examples.nativejava;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.config.CoredeuxPropertiesLoader;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.jpa.service.impl.PostgresCoredeuxJpaDataAccessService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.module.impl.AuditModuleHandler;
import com.coredeux.core.module.impl.HooksModuleHandler;
import com.coredeux.core.module.impl.ValidatorsModuleHandler;
import com.coredeux.core.registry.EntityDefinitionRegistries;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.core.resolver.EntityDefinitionBackedDataAccessResolver;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.core.service.CoredeuxModuleService;
import com.coredeux.core.service.impl.DefaultCoredeuxModuleService;
import com.coredeux.core.service.impl.DefaultCoredeuxService;
import com.coredeux.core.strategy.CoredeuxStrategy;
import com.coredeux.core.strategy.impl.DefaultCoredeuxStrategy;
import com.coredeux.examples.nativejava.module.CustomerAuditHandler;
import com.coredeux.examples.nativejava.module.CustomerLifecycleHook;
import com.coredeux.examples.nativejava.module.CustomerNameValidator;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class CoredeuxNativeRuntime implements AutoCloseable {

    private final EntityDefinitionRegistry entityDefinitionRegistry;
    private final PostgresCoredeuxJpaDataAccessService customerDataAccessService;
    private final CoredeuxService coredeuxService;
    private final CoredeuxModuleService coredeuxModuleService;

    private CoredeuxNativeRuntime(EntityDefinitionRegistry entityDefinitionRegistry,
            PostgresCoredeuxJpaDataAccessService customerDataAccessService,
            CoredeuxService coredeuxService,
            CoredeuxModuleService coredeuxModuleService) {
        this.entityDefinitionRegistry = entityDefinitionRegistry;
        this.customerDataAccessService = customerDataAccessService;
        this.coredeuxService = coredeuxService;
        this.coredeuxModuleService = coredeuxModuleService;
    }

    public static CoredeuxNativeRuntime create() {
        // Coredeux starts from a small runtime properties file. In a native Java app
        // that file lives under META-INF/coredeux.yml, similar to persistence.xml.
        CoredeuxProperties coredeuxProperties = new CoredeuxPropertiesLoader().load();

        // The runtime uses the configured entity definition location from the loaded
        // property map. This keeps core unaware of import/export-specific settings.
        EntityDefinitionRegistry registry;
        String entityConfigLocation = coredeuxProperties.string("entities.config-location");
        if (entityConfigLocation == null || entityConfigLocation.isBlank()) {
            entityConfigLocation = "classpath:coredeux-postgres-entities.yml";
        }

        try (InputStream inputStream = openStream(entityConfigLocation)) {
            registry = EntityDefinitionRegistries.fromYaml(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load Coredeux entity definitions", exception);
        }

        // The persistence unit contains sensible local PostgreSQL defaults. Environment
        // variables can override URL/user/password without changing the example code.
        Map<String, String> postgresProperties = postgresProperties();
        EntityManagerFactory entityManagerFactory = postgresProperties.isEmpty()
                ? Persistence.createEntityManagerFactory("coredeux-postgres-native")
                : Persistence.createEntityManagerFactory("coredeux-postgres-native", postgresProperties);

        // The JPA data access service is plain Java now. Passing an EntityManagerFactory
        // lets it create EntityManagers and manage resource-local transactions itself.
        PostgresCoredeuxJpaDataAccessService customerDataAccess =
                new PostgresCoredeuxJpaDataAccessService(entityManagerFactory);

        // Entity definitions refer to handlers by name. This registry is the non-Spring
        // equivalent of bean lookup: data access, validators, hooks, and audit handlers
        // are all registered under the same names used in the YAML file.
        InMemoryCoredeuxComponentRegistry components = InMemoryCoredeuxComponentRegistry.builder()
                .component("postgresCustomerDataAccess", customerDataAccess)
                .component("customerNameValidator", new CustomerNameValidator())
                .component("customerLifecycleHook", new CustomerLifecycleHook())
                .component("customerAuditHandler", new CustomerAuditHandler())
                .build();

        // Module handlers know how to execute the named validator/hook/audit components
        // at the lifecycle phases requested by the core strategy.
        List<CoredeuxEntityModuleHandler> moduleHandlers = List.of(
                new ValidatorsModuleHandler(components),
                new HooksModuleHandler(components),
                new AuditModuleHandler(components));

        // The strategy is the runtime engine behind CoredeuxService. It validates,
        // resolves data access from the entity definition, calls hooks/audit, and then
        // delegates persistence to the registered PostgreSQL data access service.
        CoredeuxStrategy strategy = new DefaultCoredeuxStrategy(registry,
                new EntityDefinitionBackedDataAccessResolver(),
                components,
                new DefaultCoredeuxReflectionHelperService(),
                () -> null,
                moduleHandlers);

        // The module service is exposed for completeness, but the demo flow uses the
        // higher-level CoredeuxService so modules run automatically during CRUD.
        CoredeuxModuleService moduleService = new DefaultCoredeuxModuleService(registry,
                new EntityDefinitionBackedDataAccessResolver(),
                components,
                new DefaultCoredeuxReflectionHelperService(),
                () -> null,
                moduleHandlers);

        return new CoredeuxNativeRuntime(registry, customerDataAccess, new DefaultCoredeuxService(strategy),
                moduleService);
    }

    public EntityDefinitionRegistry entityDefinitionRegistry() {
        // Exposes the parsed YAML model so the demo can show what Coredeux loaded.
        return entityDefinitionRegistry;
    }

    public PostgresCoredeuxJpaDataAccessService customerDataAccessService() {
        // Exposes the concrete data access service for inspection or advanced examples.
        // Normal application code should usually go through CoredeuxService instead.
        return customerDataAccessService;
    }

    public CoredeuxService coredeuxService() {
        // This is the primary facade used by applications: create, load, update,
        // search, and remove all pass through validators, hooks, audit, and storage.
        return coredeuxService;
    }

    public CoredeuxModuleService coredeuxModuleService() {
        // This lower-level service can execute configured modules directly when needed.
        // The main demo does not call it because CoredeuxService runs modules automatically.
        return coredeuxModuleService;
    }

    @Override
    public void close() {
        // Closing the runtime closes the EntityManagerFactory owned by the data access service.
        customerDataAccessService.close();
    }

    public static String customerId(String prefix) {
        // The demo uses application-assigned string ids so it can print and reload
        // the same record without relying on database-generated identifiers.
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Map<String, String> postgresProperties() {
        // Only pass overrides that exist. If none are set, persistence.xml supplies
        // the database name, username, password, driver, and Hibernate settings.
        Map<String, String> properties = new HashMap<>();
        putEnv(properties, "jakarta.persistence.jdbc.url", "COREDEUX_POSTGRES_URL");
        putEnv(properties, "jakarta.persistence.jdbc.user", "COREDEUX_POSTGRES_USER");
        putEnv(properties, "jakarta.persistence.jdbc.password", "COREDEUX_POSTGRES_PASSWORD");
        return properties;
    }

    private static void putEnv(Map<String, String> properties, String propertyName, String envName) {
        // If an environment variable is present, map it to the JPA property name
        // expected by Persistence.createEntityManagerFactory(...).
        String value = env(envName);
        if (value != null) {
            properties.put(propertyName, value);
        }
    }

    private static String env(String name) {
        // Treat missing and blank environment variables the same, so persistence.xml
        // remains the source of truth unless the developer provides a real override.
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
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
