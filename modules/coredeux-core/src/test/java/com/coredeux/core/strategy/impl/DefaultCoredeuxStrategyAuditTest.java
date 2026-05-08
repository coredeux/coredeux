package com.coredeux.core.strategy.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import com.coredeux.core.audit.CoredeuxEntityAuditHandler;
import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
import com.coredeux.core.module.impl.AuditModuleHandler;
import com.coredeux.core.module.impl.HooksModuleHandler;
import com.coredeux.core.module.impl.ValidatorsModuleHandler;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.registry.InMemoryEntityDefinitionRegistry;
import com.coredeux.core.resolver.EntityDefinitionBackedDataAccessResolver;
import com.coredeux.core.resolver.context.DefaultCoredeuxRequestContextResolver;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;

class DefaultCoredeuxStrategyAuditTest {

    @Test
    void shouldInvokeAuditHandlerForConfiguredSaveUpdateAndDeleteOperations() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingAuditHandler auditHandler = new RecordingAuditHandler();
        SampleEntity existing = new SampleEntity("1", "old-value");
        dataAccessService.existingEntities.put("1", existing);
        applicationContext.getBeanFactory().registerSingleton("customerDataAccess", dataAccessService);
        applicationContext.getBeanFactory().registerSingleton("defaultAuditHandler", auditHandler);

        DefaultCoredeuxStrategy strategy = new DefaultCoredeuxStrategy(registryWithAuditModule(),
                new EntityDefinitionBackedDataAccessResolver(), applicationContext,
                new DefaultCoredeuxReflectionHelperService(), new DefaultCoredeuxRequestContextResolver(),
                moduleHandlers(applicationContext));

        strategy.save(new SampleEntity("1", "new-save"));
        strategy.update(new SampleEntity("1", "new-update"));
        strategy.remove(new SampleEntity("1", "incoming-delete"));

        assertEquals(3, auditHandler.invocationCount);
        assertEquals(CoredeuxLifecycleOperations.UPSERT, auditHandler.operations.get(0));
        assertEquals(CoredeuxLifecycleOperations.MODIFY, auditHandler.operations.get(1));
        assertEquals(CoredeuxLifecycleOperations.DELETE, auditHandler.operations.get(2));
        assertEquals(existing, auditHandler.oldValues.get(0));
        assertEquals(existing, auditHandler.oldValues.get(1));
        assertEquals(existing, auditHandler.oldValues.get(2));
        assertEquals("1", auditHandler.identifiers.get(2));
    }

    private EntityDefinitionRegistry registryWithAuditModule() {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("customerDataAccess").build())
                .modules(List.of(CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                        .handlers(List.of("defaultAuditHandler"))
                        .config(Map.of("operations", List.of("SAVE", "UPDATE", "DELETE")))
                        .build()))
                .build();
        return new InMemoryEntityDefinitionRegistry(List.of(definition));
    }

    private List<CoredeuxEntityModuleHandler> moduleHandlers(StaticApplicationContext applicationContext) {
        return List.of(new ValidatorsModuleHandler(applicationContext), new HooksModuleHandler(applicationContext),
                new AuditModuleHandler(applicationContext));
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

        @SuppressWarnings("unused")
        public String getValue() {
            return value;
        }
    }

    private static final class RecordingDataAccessService implements CoredeuxDataAccessService {

        private final Map<String, SampleEntity> existingEntities = new HashMap<>();

        @Override
        public <T> T load(String id, Class<T> type) {
            return type.cast(existingEntities.get(id));
        }

        @Override
        public <T> String save(T entity) {
            return "saved-id";
        }

        @Override
        public <T> void update(T entity) {
        }

        @Override
        public <T> void remove(T entity) {
        }

        @Override
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
            return SearchResult.<T>builder().results(List.of()).build();
        }

        @Override
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            return SearchResult.<T>builder().results(List.of()).build();
        }
    }

    private static final class RecordingAuditHandler implements CoredeuxEntityAuditHandler<SampleEntity> {

        private int invocationCount;
        private final List<String> operations = new java.util.ArrayList<>();
        private final List<Object> identifiers = new java.util.ArrayList<>();
        private final List<Object> oldValues = new java.util.ArrayList<>();

        @Override
        public void audit(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            invocationCount++;
            EntityLifecycleContext<?> lifecycleContext = context.getLifecycleContext();
            operations.add(lifecycleContext.getOperation());
            identifiers.add(lifecycleContext.getIdentifier());
            oldValues.add(lifecycleContext.getOldValue());
        }
    }
}
