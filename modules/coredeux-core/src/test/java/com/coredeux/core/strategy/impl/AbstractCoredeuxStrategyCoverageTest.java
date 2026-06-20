package com.coredeux.core.strategy.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import com.coredeux.core.testsupport.TestComponentRegistry;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.resolver.EntityDataAccessResolver;
import com.coredeux.core.resolver.impl.DefaultCoredeuxEntityDefinitionResolver;
import com.coredeux.core.resolver.context.CoredeuxRequestContextResolver;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;
import com.coredeux.core.snapshot.impl.DefaultCoredeuxEntitySnapshotService;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;

class AbstractCoredeuxStrategyCoverageTest {

    @Test
    void shouldFailForDuplicateModuleHandlers() {
        CoredeuxEntityModuleHandler handlerOne = new NamedHandler("hooks");
        CoredeuxEntityModuleHandler handlerTwo = new NamedHandler("hooks");

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> new ExposedStrategy(registryWithDefinition(), new FixedResolver(), new TestComponentRegistry(),
                        new DefaultCoredeuxReflectionHelperService(), () -> null, List.of(handlerOne, handlerTwo)));

        assertTrue(exception.getMessage().contains("Duplicate Coredeux module handler"));
    }

    @Test
    void shouldResolveDefinitionAndCreateContext() {
        ExposedStrategy strategy = new ExposedStrategy(registryWithDefinition(), new FixedResolver(),
                new TestComponentRegistry(), new DefaultCoredeuxReflectionHelperService(),
                () -> null, List.of());
        SampleEntity entity = new SampleEntity("1", "value");

        CoredeuxEntityDefinition definition = strategy.exposedGetDefinition(SampleEntity.class);
        OperationContext context = strategy.exposedCreateOperationContext("FETCH", "1", null, entity);

        assertEquals(SampleEntity.class.getName(), definition.getFullClassName());
        assertNotNull(context.getInvokedAt());
        assertEquals("FETCH", context.getLifecycleContext().getOperation());
        assertEquals(entity, context.getLifecycleContext().getNewValue());
    }

    @Test
    void shouldCreateDefaultDefinitionWhenDefinitionIsMissing() {
        ExposedStrategy strategy = new ExposedStrategy(emptyRegistry(), new FixedResolver(),
                new TestComponentRegistry(), new DefaultCoredeuxReflectionHelperService(),
                () -> null, List.of());

        CoredeuxEntityDefinition definition = strategy.exposedGetDefinition(SampleEntity.class);

        assertEquals(SampleEntity.class.getName(), definition.getFullClassName());
        assertEquals("SampleEntity", definition.getName());
        assertNull(definition.getIdentifier());
        assertNull(definition.getStorage());
    }

    @Test
    void shouldFailWhenDataAccessBeanCannotBeResolved() {
        ExposedStrategy strategy = new ExposedStrategy(registryWithDefinition(), new FixedResolver(),
                new TestComponentRegistry(), new DefaultCoredeuxReflectionHelperService(),
                () -> null, List.of());

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> strategy.exposedGetDataAccessService(SampleEntity.class));

        assertTrue(exception.getMessage().contains("Unable to resolve CoredeuxDataAccessService bean"));
    }

    @Test
    void shouldSkipModuleExecutionWhenNoModulesOrHandlersApply() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        RecordingModuleHandler handler = new RecordingModuleHandler("hooks");
        ExposedStrategy strategy = new ExposedStrategy(registryWithDefinition(), new FixedResolver(),
                applicationContext, new DefaultCoredeuxReflectionHelperService(), () -> null, List.of(handler));
        SampleEntity entity = new SampleEntity("1", "value");
        OperationContext context = strategy.exposedCreateOperationContext("FETCH", "1", null, entity);
        CoredeuxEntityDefinition noModuleDefinition = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("customerDataAccess").build())
                .modules(List.of(
                        CoredeuxModuleDefinition.builder().name("hooks").enabled(false).handlers(List.of("x")).build(),
                        CoredeuxModuleDefinition.builder().name("validators").enabled(true).handlers(List.of("x")).build()))
                .build();

        strategy.exposedExecuteModules(entity, noModuleDefinition, "load", context);
        strategy.exposedExecuteModule(entity, noModuleDefinition, null, "load", context);

        assertEquals(0, handler.invocationCount);
    }

    @Test
    void shouldExecuteMatchingModuleHandler() {
        RecordingModuleHandler handler = new RecordingModuleHandler("hooks");
        ExposedStrategy strategy = new ExposedStrategy(registryWithDefinition(), new FixedResolver(),
                new TestComponentRegistry(), new DefaultCoredeuxReflectionHelperService(), () -> null, List.of(handler));
        CoredeuxEntityDefinition definition = registryWithDefinition().findByFullClassName(SampleEntity.class.getName()).orElseThrow();
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder().name("hooks").enabled(true).handlers(List.of("hookBean")).build();
        SampleEntity entity = new SampleEntity("1", "value");

        strategy.exposedExecuteModule(entity, definition, module, "load",
                strategy.exposedCreateOperationContext("FETCH", "1", null, entity));

        assertEquals(1, handler.invocationCount);
    }

    @Test
    void shouldInvokeLoadModulesForSingleEntityAndSearchResultEntries() {
        RecordingModuleHandler handler = new RecordingModuleHandler("hooks");
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("customerDataAccess").build())
                .modules(List.of(CoredeuxModuleDefinition.builder()
                        .name("hooks")
                        .enabled(true)
                        .handlers(List.of("hookBean"))
                        .build()))
                .build();
        ExposedStrategy strategy = new ExposedStrategy(new SimpleRegistry(List.of(definition)), new FixedResolver(),
                new TestComponentRegistry(), new DefaultCoredeuxReflectionHelperService(), () -> null, List.of(handler));

        strategy.exposedInvokeLoadModules(new SampleEntity("1", "single"), definition, "explicit-id");
        strategy.exposedInvokeLoadModules(SearchResult.<SampleEntity>builder()
                .results(List.of(new SampleEntity("2", "first"), new SampleEntity("3", "second")))
                .build(), definition);

        assertEquals(3, handler.invocationCount);
        assertEquals("explicit-id", handler.contexts.get(0).getLifecycleContext().getIdentifier());
        assertEquals(CoredeuxHookPhases.LOAD, handler.phases.get(0));
        assertEquals(CoredeuxLifecycleOperations.FETCH, handler.contexts.get(0).getLifecycleContext().getOperation());
        assertEquals("single", ((SampleEntity) handler.contexts.get(0).getLifecycleContext().getNewValue()).getValue());
        assertEquals("2", handler.contexts.get(1).getLifecycleContext().getIdentifier());
        assertEquals("3", handler.contexts.get(2).getLifecycleContext().getIdentifier());
        assertEquals(CoredeuxLifecycleOperations.FETCH, handler.contexts.get(1).getLifecycleContext().getOperation());
        assertEquals(CoredeuxLifecycleOperations.FETCH, handler.contexts.get(2).getLifecycleContext().getOperation());
    }

    @Test
    void shouldExtractAndRequireIdentifiersAndExistingState() {
        TestComponentRegistry applicationContext = new TestComponentRegistry();
        applicationContext.registerSingleton("customerDataAccess", new RecordingDataAccessService());
        ExposedStrategy strategy = new ExposedStrategy(registryWithDefinition(), new FixedResolver(), applicationContext,
                new DefaultCoredeuxReflectionHelperService(), () -> null, List.of());
        CoredeuxEntityDefinition definition = registryWithDefinition().findByFullClassName(SampleEntity.class.getName()).orElseThrow();

        assertEquals("1", strategy.exposedExtractIdentifier(new SampleEntity("1", "value"), definition));
        assertNull(strategy.exposedExtractIdentifier(new SampleEntity(" ", "value"), definition));
        assertEquals("1", strategy.exposedRequireIdentifier(new SampleEntity("1", "value"), definition, "MODIFY"));
        assertThrows(CoredeuxValidationException.class,
                () -> strategy.exposedRequireIdentifier(new SampleEntity(" ", "value"), definition, "MODIFY"));
        assertNull(strategy.exposedLoadExistingEntity(SampleEntity.class, null));
        assertEquals("loaded", strategy.exposedRequireExistingEntity(SampleEntity.class, "1", "MODIFY").getValue());
        assertThrows(CoredeuxStrategyException.class,
                () -> strategy.exposedLoadExistingEntity(MissingBeanEntity.class, "1"));
    }

    private EntityDefinitionRegistry registryWithDefinition() {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("customerDataAccess").build())
                .modules(List.of())
                .build();
        CoredeuxEntityDefinition missingBeanDefinition = CoredeuxEntityDefinition.builder()
                .fullClassName(MissingBeanEntity.class.getName())
                .name("missingBean")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("missingDataAccess").build())
                .modules(List.of())
                .build();
        return new SimpleRegistry(List.of(definition, missingBeanDefinition));
    }

    private EntityDefinitionRegistry emptyRegistry() {
        return new SimpleRegistry(List.of());
    }

    private static final class ExposedStrategy extends AbstractCoredeuxStrategy {

        private ExposedStrategy(EntityDefinitionRegistry entityDefinitionRegistry,
                EntityDataAccessResolver entityDataAccessResolver, TestComponentRegistry applicationContext,
                DefaultCoredeuxReflectionHelperService reflectionHelperService,
                CoredeuxRequestContextResolver requestContextResolver,
                List<CoredeuxEntityModuleHandler> moduleHandlers) {
            super(entityDefinitionRegistry, entityDataAccessResolver, applicationContext, reflectionHelperService,
                    requestContextResolver, new CoredeuxProperties(), new DefaultCoredeuxEntitySnapshotService(),
                    new DefaultCoredeuxEntityDefinitionResolver(entityDefinitionRegistry, new CoredeuxProperties()),
                    moduleHandlers);
        }

        private <T> CoredeuxEntityDefinition exposedGetDefinition(Class<T> entityType) {
            return getDefinition(entityType);
        }

        private <T> CoredeuxDataAccessService exposedGetDataAccessService(Class<T> entityType) {
            return getDataAccessService(entityType);
        }

        private <T> OperationContext exposedCreateOperationContext(String operation, Object identifier, T oldValue,
                T newValue) {
            return createOperationContext(operation, identifier, oldValue, newValue);
        }

        private <T> void exposedExecuteModules(T entity, CoredeuxEntityDefinition definition, String phase,
                OperationContext context) {
            executeModules(entity, definition, phase, context);
        }

        private <T> void exposedExecuteModule(T entity, CoredeuxEntityDefinition definition,
                CoredeuxModuleDefinition moduleDefinition, String phase, OperationContext context) {
            executeModule(entity, definition, moduleDefinition, phase, context);
        }

        private <T> void exposedInvokeLoadModules(T entity, CoredeuxEntityDefinition definition, Object identifier) {
            invokeLoadModules(entity, definition, identifier);
        }

        private <T> void exposedInvokeLoadModules(SearchResult<T> result, CoredeuxEntityDefinition definition) {
            invokeLoadModules(result, definition);
        }

        private <T> Object exposedExtractIdentifier(T entity, CoredeuxEntityDefinition definition) {
            return extractIdentifier(entity, definition);
        }

        private <T> Object exposedRequireIdentifier(T entity, CoredeuxEntityDefinition definition, String operation) {
            return requireIdentifier(entity, definition, operation);
        }

        private <T> T exposedLoadExistingEntity(Class<T> type, Object identifier) {
            return loadExistingEntity(type, identifier);
        }

        private <T> T exposedRequireExistingEntity(Class<T> type, Object identifier, String operation) {
            return requireExistingEntity(type, identifier, operation);
        }

    }

    private record FixedResolver() implements EntityDataAccessResolver {
        @Override
        public String resolveDataAccessService(CoredeuxEntityDefinition definition) {
            return definition.getStorage().getDataAccessService();
        }
    }

    private static final class SimpleRegistry implements EntityDefinitionRegistry {

        private final List<CoredeuxEntityDefinition> definitions;

        private SimpleRegistry(List<CoredeuxEntityDefinition> definitions) {
            this.definitions = definitions;
        }

        @Override
        public Optional<CoredeuxEntityDefinition> findByFullClassName(String fullClassName) {
            return definitions.stream().filter(def -> fullClassName.equals(def.getFullClassName())).findFirst();
        }

        @Override
        public Collection<CoredeuxEntityDefinition> getAll() {
            return definitions;
        }
    }

    private static final class NamedHandler implements CoredeuxEntityModuleHandler {
        private final String moduleName;
        private NamedHandler(String moduleName) { this.moduleName = moduleName; }
        @Override public String getModuleName() { return moduleName; }
        @Override public <T> void execute(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition, String phase, OperationContext context) { }
    }

    private static final class RecordingModuleHandler implements CoredeuxEntityModuleHandler {
        private final String moduleName;
        private int invocationCount;
        private final List<String> phases = new ArrayList<>();
        private final List<OperationContext> contexts = new ArrayList<>();
        private RecordingModuleHandler(String moduleName) { this.moduleName = moduleName; }
        @Override public String getModuleName() { return moduleName; }
        @Override public <T> void execute(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition, String phase, OperationContext context) {
            invocationCount++;
            phases.add(phase);
            contexts.add(context);
        }
    }

    private static final class RecordingDataAccessService implements CoredeuxDataAccessService {
        @Override public <T> T load(String id, Class<T> type) {
            if (type == SampleEntity.class && "1".equals(id)) {
                return type.cast(new SampleEntity("1", "loaded"));
            }
            return null;
        }
        @Override public <T> String save(T entity) { return null; }
        @Override public <T> void update(T entity) { }
        @Override public <T> void remove(T entity) { }
        @Override public <T> com.coredeux.core.search.SearchResult<T> loadAll(List<com.coredeux.core.search.SearchParams> params, Class<T> type, int pageSize, int currentPage) { return null; }
        @Override public <T> com.coredeux.core.search.SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize, int currentPage) { return null; }
    }

    private static final class SampleEntity {
        private final String id;
        private final String value;
        private SampleEntity(String id, String value) { this.id = id; this.value = value; }
        public String getId() { return id; }
        public String getValue() { return value; }
    }

    private static final class MissingBeanEntity {
        private final String id = "1";
        public String getId() { return id; }
    }
}


