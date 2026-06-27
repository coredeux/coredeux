package com.coredeux.drl.core.strategy.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.core.registry.InMemoryEntityDefinitionRegistry;
import com.coredeux.core.exceptions.CoredeuxStrategyException;
import com.coredeux.core.resolver.EntityDataAccessResolver;
import com.coredeux.core.resolver.CoredeuxEntityDefinitionResolver;
import com.coredeux.core.resolver.impl.DefaultCoredeuxEntityDefinitionResolver;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;
import com.coredeux.drl.exceptions.CoredeuxDRLException;
import com.coredeux.drl.cache.CompiledDRLRule;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;

import org.junit.jupiter.api.Test;

class DefaultDRLCoredeuxStrategyTest {

    @Test
    void routesDrlDataAccessOperationsThroughTheDrlRuntimeAndPreservesLoadModuleFanOut() {
        RecordingDrlService drlService = new RecordingDrlService();
        RecordingModuleHandler moduleHandler = new RecordingModuleHandler("hooks");
        DefaultDRLCoredeuxStrategy strategy = strategy("customerDataAccess.drl", drlService, moduleHandler);

        SampleEntity loaded = strategy.load("1", SampleEntity.class);
        SearchResult<SampleEntity> queryResult = strategy.query("from SampleEntity", Map.of(), SampleEntity.class, 10, 1);
        SearchResult<SampleEntity> loadAllResult = strategy.loadAll(List.of(), SampleEntity.class, 10, 1);
        Set<String> comparators = strategy.supportedComparators(SampleEntity.class);
        String savedIdentifier = strategy.save(new SampleEntity("1", "incoming"));
        strategy.update(new SampleEntity("1", "updated"));
        strategy.remove("1", SampleEntity.class);
        strategy.remove(new SampleEntity("1", "incoming"));
        strategy.refresh(new SampleEntity("1", "refresh"));

        assertNotNull(loaded);
        assertEquals("1", loaded.getId());
        assertEquals("loaded-from-drl", loaded.getValue());
        assertEquals(2, queryResult.getResults().size());
        assertEquals(1, loadAllResult.getResults().size());
        assertTrue(comparators.contains("eq"));
        assertEquals("drl-saved-id", savedIdentifier);
        assertEquals(13, drlService.invocations.size());
        assertEquals("customerDataAccess.drl:load", drlService.invocations.get(0));
        assertEquals(12, moduleHandler.invocationCount);
        assertEquals(5, drlService.methodCounts.getOrDefault("load", 0));
        assertEquals(1, drlService.methodCounts.getOrDefault("query", 0));
        assertEquals(1, drlService.methodCounts.getOrDefault("loadAll", 0));
        assertEquals(1, drlService.methodCounts.getOrDefault("supportedComparators", 0));
        assertEquals(1, drlService.methodCounts.getOrDefault("save", 0));
        assertEquals(1, drlService.methodCounts.getOrDefault("update", 0));
        assertEquals(2, drlService.methodCounts.getOrDefault("remove", 0));
        assertEquals(1, drlService.methodCounts.getOrDefault("refresh", 0));
        assertEquals(List.of("load", "query", "loadAll", "supportedComparators", "load", "save", "load", "update",
                "load", "remove", "load", "remove", "refresh"), drlService.lastOperationsSnapshot);
    }

    @Test
    void fallsBackToTheJavaStrategyWhenTheDataAccessBeanIsNotDrlBased() {
        RecordingJavaDataAccessService dataAccessService = new RecordingJavaDataAccessService();
        DefaultDRLCoredeuxStrategy strategy = strategy("customerDataAccess", new RecordingDrlService(),
                dataAccessService, new RecordingModuleHandler("hooks"));

        SampleEntity loaded = strategy.load("1", SampleEntity.class);
        SearchResult<SampleEntity> queryResult = strategy.query("from SampleEntity", Map.of(), SampleEntity.class, 10,
                1);
        SearchResult<SampleEntity> loadAllResult = strategy.loadAll(List.of(), SampleEntity.class, 10, 1);
        Set<String> comparators = strategy.supportedComparators(SampleEntity.class);
        String savedIdentifier = strategy.save(new SampleEntity("1", "incoming"));
        strategy.update(new SampleEntity("1", "updated"));
        strategy.remove("1", SampleEntity.class);
        strategy.remove(new SampleEntity("1", "incoming"));
        strategy.refresh(new SampleEntity("1", "refresh"));

        assertEquals("1", loaded.getId());
        assertEquals("loaded-from-java", loaded.getValue());
        assertEquals(0, queryResult.getResults().size());
        assertEquals(0, loadAllResult.getResults().size());
        assertEquals(Set.of("java-eq"), comparators);
        assertEquals("java-saved-id", savedIdentifier);
        assertEquals(6, dataAccessService.loadCount);
        assertEquals(1, dataAccessService.queryCount);
        assertEquals(1, dataAccessService.loadAllCount);
        assertEquals(1, dataAccessService.supportedComparatorsCount);
        assertEquals(1, dataAccessService.saveCount);
        assertEquals(1, dataAccessService.updateCount);
        assertEquals(2, dataAccessService.removeCount);
        assertEquals(1, dataAccessService.refreshCount);
    }

    @Test
    void fallsBackToTheJavaStrategyWhenTheDrlComparatorReturnsNull() {
        RecordingJavaDataAccessService dataAccessService = new RecordingJavaDataAccessService();
        RecordingDrlService drlService = new RecordingDrlService();
        drlService.setOutput("supportedComparators", null);
        CoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder()
                .component("coredeuxDrlService", drlService)
                .component("customerDataAccess.drl", dataAccessService)
                .build();
        EntityDefinitionRegistry definitionRegistry = new InMemoryEntityDefinitionRegistry(
                List.of(entityDefinition("customerDataAccess.drl")));
        DefaultDRLCoredeuxStrategy strategy = new DefaultDRLCoredeuxStrategy(definitionRegistry, new FixedResolver(),
                registry, new DefaultCoredeuxReflectionHelperService(), () -> null, new CoredeuxProperties(),
                definitionResolver(definitionRegistry), List.of(new RecordingModuleHandler("hooks")));

        Set<String> comparators = strategy.supportedComparators(SampleEntity.class);

        assertEquals(Set.of("java-eq"), comparators);
        assertEquals(1, dataAccessService.supportedComparatorsCount);
        assertEquals(1, drlService.methodCounts.getOrDefault("supportedComparators", 0));
    }

    @Test
    void surfacesDrlServiceResolutionFailures() {
        DefaultDRLCoredeuxStrategy strategy = strategyWithoutDrlService("customerDataAccess.drl");

        assertThrows(CoredeuxStrategyException.class, () -> strategy.load("1", SampleEntity.class));
    }

    @Test
    void surfacesDrlExecutionExceptionsEvenWhenTheRuleContextSetsAnOutput() {
        RecordingDrlService drlService = new RecordingDrlService();
        drlService.failWithContextException("load");
        DefaultDRLCoredeuxStrategy strategy = strategy("customerDataAccess.drl", drlService,
                new RecordingModuleHandler("hooks"));

        assertThrows(CoredeuxDRLException.class, () -> strategy.load("1", SampleEntity.class));
    }

    @Test
    void surfacesDrlExecutionExceptionsFromRemoveOperations() {
        RecordingDrlService drlService = new RecordingDrlService();
        drlService.failWithContextException("remove");
        DefaultDRLCoredeuxStrategy strategy = strategy("customerDataAccess.drl", drlService,
                new RecordingModuleHandler("hooks"));

        assertThrows(CoredeuxDRLException.class, () -> strategy.remove("1", SampleEntity.class));
    }

    private DefaultDRLCoredeuxStrategy strategy(String dataAccessService, DRLService drlService,
            CoredeuxEntityModuleHandler... handlers) {
        return strategy(dataAccessService, drlService, new RecordingJavaDataAccessService(), handlers);
    }

    private DefaultDRLCoredeuxStrategy strategy(String dataAccessService, DRLService drlService,
            RecordingJavaDataAccessService javaDataAccessService, CoredeuxEntityModuleHandler... handlers) {
        CoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder()
                .component("coredeuxDrlService", drlService)
                .component("customerDataAccess", javaDataAccessService)
                .build();
        EntityDefinitionRegistry definitionRegistry = new InMemoryEntityDefinitionRegistry(List.of(entityDefinition(dataAccessService)));
        List<CoredeuxEntityModuleHandler> moduleHandlers = new ArrayList<>();
        moduleHandlers.addAll(List.of(handlers));
        return new DefaultDRLCoredeuxStrategy(definitionRegistry, new FixedResolver(), registry,
                new DefaultCoredeuxReflectionHelperService(), () -> null, new CoredeuxProperties(),
                definitionResolver(definitionRegistry), moduleHandlers);
    }

    private DefaultDRLCoredeuxStrategy strategyWithoutDrlService(String dataAccessService) {
        CoredeuxComponentRegistry registry = InMemoryCoredeuxComponentRegistry.builder()
                .component("customerDataAccess", new RecordingJavaDataAccessService())
                .build();
        EntityDefinitionRegistry definitionRegistry = new InMemoryEntityDefinitionRegistry(List.of(entityDefinition(dataAccessService)));
        return new DefaultDRLCoredeuxStrategy(definitionRegistry, new FixedResolver(), registry,
                new DefaultCoredeuxReflectionHelperService(), () -> null, new CoredeuxProperties(),
                definitionResolver(definitionRegistry), List.of(new RecordingModuleHandler("hooks")));
    }

    private CoredeuxEntityDefinitionResolver definitionResolver(EntityDefinitionRegistry definitionRegistry) {
        return new DefaultCoredeuxEntityDefinitionResolver(definitionRegistry, new CoredeuxProperties());
    }

    private CoredeuxEntityDefinition entityDefinition(String dataAccessService) {
        return CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService(dataAccessService).build())
                .modules(List.of(CoredeuxModuleDefinition.builder()
                        .name("hooks")
                        .enabled(true)
                        .handlers(List.of("recording-handler"))
                        .build()))
                .build();
    }

    private static final class FixedResolver implements EntityDataAccessResolver {
        @Override
        public String resolveDataAccessService(CoredeuxEntityDefinition definition) {
            return definition.getStorage().getDataAccessService();
        }
    }

    private static final class RecordingJavaDataAccessService implements CoredeuxDataAccessService {

        private int loadCount;
        private int queryCount;
        private int loadAllCount;
        private int supportedComparatorsCount;
        private int saveCount;
        private int updateCount;
        private int removeCount;
        private int refreshCount;

        @Override
        public <T> T load(String id, Class<T> type) {
            loadCount++;
            if (type == SampleEntity.class) {
                return type.cast(new SampleEntity(id, "loaded-from-java"));
            }
            return null;
        }

        @Override
        public <T> String save(T entity) {
            saveCount++;
            return "java-saved-id";
        }

        @Override
        public <T> void update(T entity) {
            updateCount++;
        }

        @Override
        public <T> void remove(T entity) {
            removeCount++;
        }

        @Override
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
            loadAllCount++;
            return SearchResult.<T>builder().results(List.of()).build();
        }

        @Override
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            queryCount++;
            return SearchResult.<T>builder().results(List.of()).build();
        }

        @Override
        public Set<String> supportedComparators(Class<?> type) {
            supportedComparatorsCount++;
            return Set.of("java-eq");
        }

        @Override
        public <T> void refresh(T entity) {
            refreshCount++;
        }
    }

    private static final class RecordingModuleHandler implements CoredeuxEntityModuleHandler {

        private final String moduleName;
        private int invocationCount;

        private RecordingModuleHandler(String moduleName) {
            this.moduleName = moduleName;
        }

        @Override
        public String getModuleName() {
            return moduleName;
        }

        @Override
        public <T> void execute(T entity, CoredeuxEntityDefinition definition, CoredeuxModuleDefinition moduleDefinition,
                String phase, OperationContext context) {
            invocationCount++;
        }
    }

    private static final class RecordingDrlService implements DRLService {

        private final Map<String, Integer> methodCounts = new HashMap<>();
        private final List<String> invocations = new ArrayList<>();
        private final List<String> lastOperationsSnapshot = new ArrayList<>();
        private final Map<String, Object> fixedOutputs = new HashMap<>();
        private final List<String> failingMethods = new ArrayList<>();

        private void setOutput(String method, Object output) {
            fixedOutputs.put(method, output);
        }

        private void failWithContextException(String method) {
            failingMethods.add(method);
        }

        @Override
        public <T> void execute(String ruleId, RuleContext<T> context) {
            executeInternal(ruleId, context);
        }

        @Override
        public <T> void execute(String ruleId, String source, RuleContext<T> context) {
            executeInternal(ruleId, context);
        }

        @Override
        public <T> void executeSource(String source, RuleContext<T> context) {
            executeInternal("inline", context);
        }

        @Override
        public void purgeCache() {
        }

        @Override
        public void purgeCache(String ruleId) {
        }

        @Override
        public boolean isCached(String ruleId) {
            return false;
        }

        @Override
        public void compileAndCache(String ruleId) {
        }

        @Override
        public void compileAndCache(String ruleId, String source) {
        }

        @Override
        public CompiledDRLRule compile(String ruleId) {
            return new CompiledDRLRule(null, false);
        }

        @Override
        public CompiledDRLRule compile(String ruleId, String drl) {
            return new CompiledDRLRule(null, false);
        }

        @SuppressWarnings("unchecked")
        private <T> void executeInternal(String ruleId, RuleContext<T> context) {
            String method = context.getMethod();
            invocations.add(ruleId + ":" + method);
            methodCounts.merge(method, 1, Integer::sum);
            lastOperationsSnapshot.add(method);

            if (failingMethods.contains(method)) {
                context.setOutput((T) new SampleEntity("boom", "boom"));
                context.setException(new IllegalStateException("boom"));
                return;
            }

            if (fixedOutputs.containsKey(method)) {
                context.setOutput((T) fixedOutputs.get(method));
                return;
            }

            switch (method) {
                case "load" -> context.setOutput((T) new SampleEntity(
                        String.valueOf(context.getParams().getOrDefault("identifier", context.getParams().get("id"))),
                        "loaded-from-drl"));
                case "query" -> context.setOutput((T) SearchResult.<SampleEntity>builder()
                        .results(List.of(new SampleEntity("1", "one"), new SampleEntity("2", "two")))
                        .build());
                case "loadAll" -> context.setOutput((T) SearchResult.<SampleEntity>builder()
                        .results(List.of(new SampleEntity("3", "three")))
                        .build());
                case "supportedComparators" -> context.setOutput((T) Set.of("eq", "like"));
                case "save" -> context.setOutput((T) "drl-saved-id");
                case "update", "remove", "refresh" -> {
                    // no-op; the strategy only needs the side effects and the invocation.
                }
                default -> throw new IllegalStateException("Unexpected method: " + method);
            }
        }
    }

    private static final class SampleEntity {

        private final String id;
        private final String value;

        private SampleEntity(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }
}
