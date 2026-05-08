package com.coredeux.core.strategy.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.hooks.CoredeuxEntityHook;
import com.coredeux.core.module.CoredeuxEntityModuleHandler;
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
import com.coredeux.core.validation.CoredeuxEntityValidator;
import com.coredeux.core.validation.ValidationError;

class DefaultCoredeuxStrategyTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldInvokeEnabledValidatorAndHookModulesDuringSaveWithFrameworkManagedLifecycleState() {
        MockHttpServletRequest request = request("req-1", "corr-1", "user-1", "site-1");
        request.setPreferredLocales(List.of(Locale.ENGLISH));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingValidator validator = new RecordingValidator();
        RecordingHook hook = new RecordingHook();
        SampleEntity existing = new SampleEntity("1", "old-value");

        dataAccessService.existingEntities.put("1", existing);
        registerBeans(applicationContext, dataAccessService, validator, hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                        .handlers(List.of("customerValidator")).build(),
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        SampleEntity incoming = new SampleEntity("1", "new-value");
        String id = strategy.save(incoming);

        assertEquals("saved-id", id);
        assertTrue(dataAccessService.saveInvoked);
        assertNotNull(validator.beforeSaveOperationContext);
        assertNotNull(validator.beforeSaveOperationContext.getInvokedAt());
        assertEquals("req-1", validator.beforeSaveOperationContext.getRequestContext().getRequestId());
        assertEquals("site-1", validator.beforeSaveOperationContext.getRequestContext().getTenantId());
        assertNotNull(validator.beforeSaveOperationContext.getLifecycleContext());
        assertEquals(CoredeuxLifecycleOperations.CREATE,
                validator.beforeSaveOperationContext.getLifecycleContext().getOperation());
        assertEquals(CoredeuxLifecycleOperations.CREATE, validator.beforeSaveContext.getOperation());
        assertEquals("1", validator.beforeSaveContext.getIdentifier());
        assertEquals(existing, validator.beforeSaveContext.getOldValue());
        assertEquals(incoming, validator.beforeSaveContext.getNewValue());
        assertNotNull(hook.beforeSaveOperationContext);
        assertEquals(CoredeuxLifecycleOperations.CREATE,
                hook.beforeSaveOperationContext.getLifecycleContext().getOperation());
        assertEquals("corr-1", hook.beforeSaveOperationContext.getRequestContext().getCorrelationId());
        assertEquals(CoredeuxLifecycleOperations.CREATE, hook.beforeSaveContext.getOperation());
        assertEquals(CoredeuxLifecycleOperations.UPSERT, hook.afterSaveContext.getOperation());
        assertEquals(existing, hook.afterSaveContext.getOldValue());
        assertEquals(incoming, hook.afterSaveContext.getNewValue());
    }

    @Test
    void shouldFallbackToOriginalIdentifierWhenSaveReturnsBlankIdentifier() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        dataAccessService.persistedIdentifier = " ";
        RecordingHook hook = new RecordingHook();
        SampleEntity existing = new SampleEntity("1", "old-value");
        dataAccessService.existingEntities.put("1", existing);
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        String id = strategy.save(new SampleEntity("1", "new-value"));

        assertEquals(" ", id);
        assertEquals("1", hook.afterSaveContext.getIdentifier());
    }

    @Test
    void shouldApplyFetchLifecycleContextDuringLoad() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request("req-fetch", null, null, null)));

        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        SampleEntity existing = new SampleEntity("11", "loaded");

        dataAccessService.existingEntities.put("11", existing);
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        SampleEntity loaded = strategy.load("11", SampleEntity.class);

        assertEquals(existing, loaded);
        assertNotNull(hook.loadOperationContext);
        assertNotNull(hook.loadOperationContext.getInvokedAt());
        assertEquals("req-fetch", hook.loadOperationContext.getRequestContext().getRequestId());
        assertNotNull(hook.loadOperationContext.getLifecycleContext());
        assertEquals(CoredeuxLifecycleOperations.FETCH, hook.loadOperationContext.getLifecycleContext().getOperation());
        assertNotNull(hook.loadContext);
        assertEquals(CoredeuxLifecycleOperations.FETCH, hook.loadContext.getOperation());
        assertEquals("11", hook.loadContext.getIdentifier());
        assertNull(hook.loadContext.getOldValue());
        assertEquals(existing, hook.loadContext.getNewValue());
    }

    @Test
    void shouldUseRequestedIdWhenLoadedEntityIdentifierIsNull() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        SampleEntity existing = new SampleEntity(null, "loaded");
        dataAccessService.existingEntities.put("11", existing);
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        SampleEntity loaded = strategy.load("11", SampleEntity.class);

        assertEquals(existing, loaded);
        assertEquals("11", hook.loadContext.getIdentifier());
    }

    @Test
    void shouldReturnNullWithoutInvokingLoadModulesWhenEntityNotFound() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        SampleEntity loaded = strategy.load("missing", SampleEntity.class);

        assertNull(loaded);
        assertEquals(0, hook.loadInvocationCount);
    }

    @Test
    void shouldInvokeLoadModulesForQueryAndLoadAllResults() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        SampleEntity entityOne = new SampleEntity("1", "one");
        SampleEntity entityTwo = new SampleEntity("2", "two");
        dataAccessService.queryResult = SearchResult.<SampleEntity>builder().results(List.of(entityOne, entityTwo)).build();
        dataAccessService.loadAllResult = SearchResult.<SampleEntity>builder().results(List.of(entityTwo)).build();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        SearchResult<SampleEntity> queryResult = strategy.query("from SampleEntity", Map.of(), SampleEntity.class, 10, 1);
        SearchResult<SampleEntity> loadAllResult = strategy.loadAll(List.of(), SampleEntity.class, 10, 1);

        assertSame(dataAccessService.queryResult, queryResult);
        assertSame(dataAccessService.loadAllResult, loadAllResult);
        assertEquals(3, hook.loadInvocationCount);
    }

    @Test
    void shouldIgnoreNullQueryAndNullEntitiesDuringLoadModuleInvocation() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        dataAccessService.queryResult = null;
        dataAccessService.loadAllResult = SearchResult.<SampleEntity>builder()
                .results(java.util.Arrays.asList((SampleEntity) null)).build();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        assertNull(strategy.query("from SampleEntity", Map.of(), SampleEntity.class, 10, 1));
        SearchResult<SampleEntity> loadAllResult = strategy.loadAll(List.of(), SampleEntity.class, 10, 1);

        assertNotNull(loadAllResult);
        assertEquals(0, hook.loadInvocationCount);
    }

    @Test
    void shouldIgnoreSearchResultWithNullResults() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        dataAccessService.loadAllResult = SearchResult.<SampleEntity>builder().results(null).build();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        SearchResult<SampleEntity> loadAllResult = strategy.loadAll(List.of(), SampleEntity.class, 10, 1);

        assertNotNull(loadAllResult);
        assertNull(loadAllResult.getResults());
        assertEquals(0, hook.loadInvocationCount);
    }

    @Test
    void shouldInvokeDeleteFlowForRemoveByIdAndEntity() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        SampleEntity existing = new SampleEntity("7", "stored");
        dataAccessService.existingEntities.put("7", existing);
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        strategy.remove("7", SampleEntity.class);
        strategy.remove(new SampleEntity("7", "incoming"));

        assertEquals(2, hook.beforeDeleteInvocationCount);
        assertEquals(2, dataAccessService.removeInvocationCount);
        assertEquals(existing, hook.beforeDeleteContext.getOldValue());
        assertNull(hook.beforeDeleteContext.getNewValue());
    }

    @Test
    void shouldInvokeRefreshModulesAroundDataAccessRefresh() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        SampleEntity entity = new SampleEntity("9", "refresh");
        strategy.refresh(entity);

        assertEquals(1, hook.beforeRefreshInvocationCount);
        assertEquals(1, hook.afterRefreshInvocationCount);
        assertEquals(1, dataAccessService.refreshInvocationCount);
        assertEquals(CoredeuxLifecycleOperations.FETCH, hook.beforeRefreshContext.getOperation());
        assertEquals(entity, hook.beforeRefreshContext.getNewValue());
    }

    @Test
    void shouldUpdateExistingEntityAndInvokeUpdateModules() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        SampleEntity existing = new SampleEntity("15", "old");
        SampleEntity incoming = new SampleEntity("15", "new");
        dataAccessService.existingEntities.put("15", existing);
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        DefaultCoredeuxStrategy strategy = strategy(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        strategy.update(incoming);

        assertEquals(1, dataAccessService.updateInvocationCount);
        assertEquals(1, hook.beforeUpdateInvocationCount);
        assertEquals(1, hook.afterUpdateInvocationCount);
        assertEquals(CoredeuxLifecycleOperations.MODIFY, hook.beforeUpdateContext.getOperation());
        assertEquals(existing, hook.beforeUpdateContext.getOldValue());
        assertEquals(incoming, hook.beforeUpdateContext.getNewValue());
        assertEquals(CoredeuxLifecycleOperations.MODIFY, hook.afterUpdateContext.getOperation());
    }

    @Test
    void shouldFailUpdateWhenExistingEntityCannotBeResolved() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), new RecordingHook());

        DefaultCoredeuxStrategy strategy = strategy(applicationContext);

        CoredeuxDataAccessException exception = assertThrows(CoredeuxDataAccessException.class,
                () -> strategy.update(new SampleEntity("99", "missing")));

        assertTrue(exception.getMessage().contains("MODIFY"));
    }

    @Test
    void shouldFailRemoveWhenExistingEntityCannotBeResolved() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), new RecordingHook());

        DefaultCoredeuxStrategy strategy = strategy(applicationContext);

        CoredeuxDataAccessException exception = assertThrows(CoredeuxDataAccessException.class,
                () -> strategy.remove("99", SampleEntity.class));

        assertTrue(exception.getMessage().contains("DELETE"));
    }

    private DefaultCoredeuxStrategy strategy(StaticApplicationContext applicationContext,
            CoredeuxModuleDefinition... modules) {
        return new DefaultCoredeuxStrategy(registryWithModules(modules),
                new EntityDefinitionBackedDataAccessResolver(), applicationContext,
                new DefaultCoredeuxReflectionHelperService(), new DefaultCoredeuxRequestContextResolver(),
                moduleHandlers(applicationContext));
    }

    private void registerBeans(StaticApplicationContext applicationContext, RecordingDataAccessService dataAccessService,
            RecordingValidator validator, RecordingHook hook) {
        applicationContext.getBeanFactory().registerSingleton("customerDataAccess", dataAccessService);
        applicationContext.getBeanFactory().registerSingleton("customerValidator", validator);
        applicationContext.getBeanFactory().registerSingleton("customerLifecycleHook", hook);
    }

    private MockHttpServletRequest request(String requestId, String correlationId, String userId, String siteId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (requestId != null) {
            request.addHeader("X-Request-Id", requestId);
        }
        if (correlationId != null) {
            request.addHeader("X-Correlation-Id", correlationId);
        }
        if (userId != null) {
            request.addHeader("X-User-Id", userId);
        }
        if (siteId != null) {
            request.addHeader("X-Site-Id", siteId);
        }
        return request;
    }

    private EntityDefinitionRegistry registryWithModules(CoredeuxModuleDefinition... modules) {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("customerDataAccess").build())
                .modules(List.of(modules))
                .build();
        return new InMemoryEntityDefinitionRegistry(List.of(definition));
    }

    private List<CoredeuxEntityModuleHandler> moduleHandlers(StaticApplicationContext applicationContext) {
        return List.of(new ValidatorsModuleHandler(applicationContext), new HooksModuleHandler(applicationContext));
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
        private boolean saveInvoked;
        private String persistedIdentifier = "saved-id";
        private int updateInvocationCount;
        private int removeInvocationCount;
        private int refreshInvocationCount;
        private SearchResult<SampleEntity> queryResult = SearchResult.<SampleEntity>builder().results(List.of()).build();
        private SearchResult<SampleEntity> loadAllResult = SearchResult.<SampleEntity>builder().results(List.of()).build();
        private final List<String> lastLoadedIds = new java.util.ArrayList<>();

        @Override
        public <T> T load(String id, Class<T> type) {
            lastLoadedIds.add(id);
            return type.cast(existingEntities.get(id));
        }

        @Override
        public <T> String save(T entity) {
            saveInvoked = true;
            return persistedIdentifier;
        }

        @Override
        public <T> void update(T entity) {
            updateInvocationCount++;
        }

        @Override
        public <T> void remove(T entity) {
            removeInvocationCount++;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
            return (SearchResult<T>) loadAllResult;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            return (SearchResult<T>) queryResult;
        }

        @Override
        public <T> void refresh(T entity) {
            refreshInvocationCount++;
        }
    }

    private static final class RecordingValidator implements CoredeuxEntityValidator<SampleEntity> {

        private OperationContext beforeSaveOperationContext;
        private EntityLifecycleContext<?> beforeSaveContext;

        @Override
        public List<ValidationError> validate(SampleEntity entity, CoredeuxEntityDefinition definition,
                OperationContext context) {
            beforeSaveOperationContext = context;
            beforeSaveContext = context.getLifecycleContext();
            return List.of();
        }
    }

    private static final class RecordingHook implements CoredeuxEntityHook<SampleEntity> {

        private int loadInvocationCount;
        private int beforeDeleteInvocationCount;
        private int beforeRefreshInvocationCount;
        private int afterRefreshInvocationCount;
        private int beforeUpdateInvocationCount;
        private int afterUpdateInvocationCount;
        private OperationContext loadOperationContext;
        private OperationContext beforeSaveOperationContext;
        private EntityLifecycleContext<?> loadContext;
        private EntityLifecycleContext<?> beforeSaveContext;
        private EntityLifecycleContext<?> afterSaveContext;
        private EntityLifecycleContext<?> beforeDeleteContext;
        private EntityLifecycleContext<?> beforeRefreshContext;
        private EntityLifecycleContext<?> beforeUpdateContext;
        private EntityLifecycleContext<?> afterUpdateContext;

        @Override
        public void onLoad(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            loadInvocationCount++;
            loadOperationContext = context;
            loadContext = context.getLifecycleContext();
        }

        @Override
        public void beforeSave(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            beforeSaveOperationContext = context;
            beforeSaveContext = context.getLifecycleContext();
        }

        @Override
        public void afterSave(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            afterSaveContext = context.getLifecycleContext();
        }

        @Override
        public void beforeUpdate(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            beforeUpdateInvocationCount++;
            beforeUpdateContext = context.getLifecycleContext();
        }

        @Override
        public void afterUpdate(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            afterUpdateInvocationCount++;
            afterUpdateContext = context.getLifecycleContext();
        }

        @Override
        public void beforeDelete(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            beforeDeleteInvocationCount++;
            beforeDeleteContext = context.getLifecycleContext();
        }

        @Override
        public void beforeRefresh(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            beforeRefreshInvocationCount++;
            beforeRefreshContext = context.getLifecycleContext();
        }

        @Override
        public void afterRefresh(SampleEntity entity, CoredeuxEntityDefinition definition, OperationContext context) {
            afterRefreshInvocationCount++;
        }
    }
}
