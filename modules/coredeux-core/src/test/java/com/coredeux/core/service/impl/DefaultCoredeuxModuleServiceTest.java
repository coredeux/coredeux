package com.coredeux.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
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
import com.coredeux.core.service.CoredeuxModuleService;
import com.coredeux.core.strategy.CoredeuxHookPhases;
import com.coredeux.core.strategy.CoredeuxLifecycleOperations;
import com.coredeux.core.validation.CoredeuxEntityValidator;
import com.coredeux.core.validation.ValidationError;

class DefaultCoredeuxModuleServiceTest {

    @Test
    void shouldExecuteAllConfiguredModules() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingValidator validator = new RecordingValidator();
        RecordingHook hook = new RecordingHook();
        SampleEntity existing = new SampleEntity("1", "old-value");

        dataAccessService.existingEntities.put("1", existing);
        registerBeans(applicationContext, dataAccessService, validator, hook);

        CoredeuxModuleService moduleService = moduleService(applicationContext,
                CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                        .handlers(List.of("customerValidator")).build(),
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        SampleEntity entity = new SampleEntity("1", "new-value");
        moduleService.executeAll(entity, CoredeuxHookPhases.BEFORE_UPDATE, CoredeuxLifecycleOperations.MODIFY);

        assertEquals(1, validator.invocationCount);
        assertEquals(1, hook.beforeUpdateInvocationCount);
        assertNotNull(validator.context);
        assertEquals(CoredeuxLifecycleOperations.MODIFY, validator.context.getLifecycleContext().getOperation());
        assertEquals(existing, validator.context.getLifecycleContext().getOldValue());
        assertEquals(entity, validator.context.getLifecycleContext().getNewValue());
    }

    @Test
    void shouldExecuteOnlyRequestedModule() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingValidator validator = new RecordingValidator();
        RecordingHook hook = new RecordingHook();
        SampleEntity existing = new SampleEntity("1", "old-value");

        dataAccessService.existingEntities.put("1", existing);
        registerBeans(applicationContext, dataAccessService, validator, hook);

        CoredeuxModuleService moduleService = moduleService(applicationContext,
                CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                        .handlers(List.of("customerValidator")).build(),
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        moduleService.executeModule(new SampleEntity("1", "new-value"), "hooks", CoredeuxHookPhases.BEFORE_UPDATE,
                CoredeuxLifecycleOperations.MODIFY);

        assertEquals(0, validator.invocationCount);
        assertEquals(1, hook.beforeUpdateInvocationCount);
    }

    @Test
    void shouldExecuteDeleteModulesAgainstExistingSnapshot() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingHook hook = new RecordingHook();
        SampleEntity existing = new SampleEntity("1", "old-value");

        dataAccessService.existingEntities.put("1", existing);
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), hook);

        CoredeuxModuleService moduleService = moduleService(applicationContext,
                CoredeuxModuleDefinition.builder().name("hooks").enabled(true)
                        .handlers(List.of("customerLifecycleHook")).build());

        moduleService.executeAll(new SampleEntity("1", "incoming"), CoredeuxHookPhases.BEFORE_DELETE,
                CoredeuxLifecycleOperations.DELETE);

        assertEquals(1, hook.beforeDeleteInvocationCount);
        assertEquals(existing, hook.executedEntity);
        assertEquals(existing, hook.beforeDeleteContext.getOldValue());
        assertNull(hook.beforeDeleteContext.getNewValue());
    }

    @Test
    void shouldAllowCreateExecutionWithoutExistingState() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        RecordingValidator validator = new RecordingValidator();
        registerBeans(applicationContext, dataAccessService, validator, new RecordingHook());

        CoredeuxModuleService moduleService = moduleService(applicationContext,
                CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                        .handlers(List.of("customerValidator")).build());

        SampleEntity entity = new SampleEntity(null, "new-value");
        moduleService.executeAll(entity, CoredeuxHookPhases.BEFORE_SAVE, CoredeuxLifecycleOperations.CREATE);

        assertEquals(1, validator.invocationCount);
        assertNull(validator.context.getLifecycleContext().getOldValue());
        assertNull(validator.context.getLifecycleContext().getIdentifier());
        assertEquals(entity, validator.context.getLifecycleContext().getNewValue());
    }

    @Test
    void shouldFailWhenRequestedModuleIsNotConfigured() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), new RecordingHook());

        CoredeuxModuleService moduleService = moduleService(applicationContext);

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> moduleService.executeModule(new SampleEntity("1", "new-value"), "hooks",
                        CoredeuxHookPhases.BEFORE_UPDATE, CoredeuxLifecycleOperations.MODIFY));

        assertTrue(exception.getMessage().contains("No enabled module named 'hooks'"));
    }

    @Test
    void shouldFailStrictOperationsWhenExistingEntityCannotBeResolved() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), new RecordingHook());

        CoredeuxModuleService moduleService = moduleService(applicationContext);

        CoredeuxDataAccessException exception = assertThrows(CoredeuxDataAccessException.class,
                () -> moduleService.executeAll(new SampleEntity("99", "missing"), CoredeuxHookPhases.BEFORE_UPDATE,
                        CoredeuxLifecycleOperations.MODIFY));

        assertTrue(exception.getMessage().contains("MODIFY"));
    }

    @Test
    void shouldFailForInvalidExternalExecutionRequests() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        RecordingDataAccessService dataAccessService = new RecordingDataAccessService();
        registerBeans(applicationContext, dataAccessService, new RecordingValidator(), new RecordingHook());

        CoredeuxModuleService moduleService = moduleService(applicationContext);

        assertThrows(CoredeuxValidationException.class,
                () -> moduleService.executeAll(null, CoredeuxHookPhases.BEFORE_SAVE, CoredeuxLifecycleOperations.CREATE));
        assertThrows(CoredeuxValidationException.class,
                () -> moduleService.executeAll(new SampleEntity("1", "x"), " ", CoredeuxLifecycleOperations.CREATE));
        assertThrows(CoredeuxValidationException.class,
                () -> moduleService.executeAll(new SampleEntity("1", "x"), CoredeuxHookPhases.BEFORE_SAVE, " "));
        assertThrows(CoredeuxValidationException.class,
                () -> moduleService.executeModule(new SampleEntity("1", "x"), " ", CoredeuxHookPhases.BEFORE_SAVE,
                        CoredeuxLifecycleOperations.CREATE));
    }

    private CoredeuxModuleService moduleService(StaticApplicationContext applicationContext,
            CoredeuxModuleDefinition... modules) {
        return new DefaultCoredeuxModuleService(registryWithModules(modules),
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

        @Override
        public <T> T load(String id, Class<T> type) {
            return type.cast(existingEntities.get(id));
        }

        @Override
        public <T> String save(T entity) {
            return null;
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

    private static final class RecordingValidator implements CoredeuxEntityValidator<SampleEntity> {

        private int invocationCount;
        private OperationContext context;

        @Override
        public List<ValidationError> validate(SampleEntity entity, CoredeuxEntityDefinition definition,
                OperationContext context) {
            invocationCount++;
            this.context = context;
            return List.of();
        }
    }

    private static final class RecordingHook implements CoredeuxEntityHook<SampleEntity> {

        private int beforeUpdateInvocationCount;
        private int beforeDeleteInvocationCount;
        private Object executedEntity;
        private EntityLifecycleContext<?> beforeDeleteContext;

        @Override
        public void beforeUpdate(SampleEntity entity, CoredeuxEntityDefinition definition,
                OperationContext operationContext) {
            beforeUpdateInvocationCount++;
        }

        @Override
        public void beforeDelete(SampleEntity entity, CoredeuxEntityDefinition definition,
                OperationContext operationContext) {
            beforeDeleteInvocationCount++;
            executedEntity = entity;
            beforeDeleteContext = operationContext.getLifecycleContext();
        }
    }
}
