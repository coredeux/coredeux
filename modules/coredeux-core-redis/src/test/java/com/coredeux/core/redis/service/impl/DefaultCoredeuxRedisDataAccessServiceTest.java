package com.coredeux.core.redis.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.redis.testentity.SampleRedisEntity;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

class DefaultCoredeuxRedisDataAccessServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> commands;
    private DefaultCoredeuxRedisDataAccessService dataAccessService;
    private Map<String, String> store;

    @BeforeEach
    void setUp() {
        connection = mock(StatefulRedisConnection.class);
        commands = mock(RedisCommands.class);
        store = new LinkedHashMap<>();
        when(connection.sync()).thenReturn(commands);
        when(commands.set(anyString(), anyString())).thenAnswer(invocation -> {
            store.put(invocation.getArgument(0), invocation.getArgument(1));
            return "OK";
        });
        when(commands.get(anyString())).thenAnswer(invocation -> store.get(invocation.getArgument(0)));
        when(commands.del(anyString())).thenAnswer(invocation -> store.remove(invocation.getArgument(0)) != null ? 1L : 0L);
        when(commands.keys(anyString())).thenAnswer(invocation -> {
            String pattern = invocation.getArgument(0);
            Set<String> keys = new HashSet<>();
            String regex = pattern.replace("*", ".*");
            for (String key : store.keySet()) {
                if (key.matches(regex)) {
                    keys.add(key);
                }
            }
            return new ArrayList<>(keys);
        });
        when(commands.incr(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            long value = Long.parseLong(store.getOrDefault(key, "0")) + 1L;
            store.put(key, String.valueOf(value));
            return value;
        });
        dataAccessService = new DefaultCoredeuxRedisDataAccessService(connection, "");
    }

    @Test
    void shouldSaveLoadUpdateRemoveAndRefreshEntity() throws Exception {
        SampleRedisEntity created = sample(null, "Alpha", 10, "one");
        String id = dataAccessService.save(created);

        assertNotNull(id);
        assertEquals(id, created.getId());

        SampleRedisEntity loaded = dataAccessService.load(id, SampleRedisEntity.class);
        assertEquals("Alpha", loaded.getName());

        SampleRedisEntity update = sample(id, "Alpha Updated", 15, "two");
        dataAccessService.update(update);

        SampleRedisEntity persisted = OBJECT_MAPPER.readValue(store.get(redisKey(id)), SampleRedisEntity.class);
        assertEquals("Alpha Updated", persisted.getName());

        store.put(redisKey(id), OBJECT_MAPPER.writeValueAsString(sample(id, "Mutated", 20, "three")));
        dataAccessService.refresh(update);
        assertEquals("Mutated", update.getName());

        dataAccessService.remove(update);
        assertFalse(store.containsKey(redisKey(id)));
    }

    @Test
    void shouldLoadAllUsingSupportedComparatorsAndPagination() {
        persistSamples();

        SearchResult<SampleRedisEntity> equalsResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("EQUALS").value("Alpha").build()),
                SampleRedisEntity.class, 10, 1);
        assertEquals(1, equalsResult.getResults().size());
        assertEquals(3L, equalsResult.getPagination().getTotalResults());
        assertEquals(1L, equalsResult.getPagination().getResultSize());

        SearchResult<SampleRedisEntity> greaterResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("age").comparator("GREATERTHAN").value(10).build()),
                SampleRedisEntity.class, 1, 1);
        assertEquals(1, greaterResult.getResults().size());
        assertEquals(2L, greaterResult.getPagination().getResultSize());
        assertEquals(2L, greaterResult.getPagination().getTotalPages());
    }

    @Test
    void shouldQueryWithJsonFiltersAndPagination() {
        persistSamples();

        SearchResult<SampleRedisEntity> result = dataAccessService.query(
                """
                        {"filters":[{"field":"name","comparator":"ANYWHERE","value":"ha"}]}
                        """,
                Map.of(), SampleRedisEntity.class, 1, 1);

        assertEquals(1, result.getResults().size());
        assertEquals(3L, result.getPagination().getTotalResults());
        assertEquals(1L, result.getPagination().getResultSize());
    }

    @Test
    void shouldRejectInvalidInputs() {
        assertThrows(CoredeuxValidationException.class, () -> dataAccessService.load(null, SampleRedisEntity.class));
        assertThrows(CoredeuxValidationException.class, () -> dataAccessService.load("1", null));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.query(" ", Map.of(), SampleRedisEntity.class, 10, 1));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.loadAll(
                        List.of(SearchParams.builder().field("name").comparator("UNKNOWN").value("x").build()),
                        SampleRedisEntity.class, 10, 1));
    }

    @Test
    void shouldWrapRuntimeFailures() {
        when(commands.get(anyString())).thenThrow(new IllegalStateException("boom"));
        assertThrows(CoredeuxDataAccessException.class,
                () -> dataAccessService.load("1", SampleRedisEntity.class));
    }

    @Test
    void shouldAdvertiseRedisComparators() {
        assertFalse(dataAccessService.supportedComparators(SampleRedisEntity.class).isEmpty());
        assertTrue(dataAccessService.supportedComparators(SampleRedisEntity.class).contains("ANYWHERE"));
    }

    @Test
    void shouldSupportUnpagedOperations() {
        persistSamples();

        SearchResult<SampleRedisEntity> result = dataAccessService.loadAll(List.of(), SampleRedisEntity.class, -1, -1);
        assertEquals(3, result.getResults().size());
        assertEquals(-1L, result.getPagination().getCurrentPage());
        assertEquals(-1L, result.getPagination().getPageSize());
        assertNull(result.getPagination().getTotalResults());
    }

    @Test
    void shouldAllowSavingNewEntityWithGeneratedStringIdentifier() {
        SampleRedisEntity created = sample(null, "Alpha", 10, "one");
        String id = dataAccessService.save(created);
        assertNotNull(id);
        assertNotNull(created.getId());
    }

    @Test
    void shouldCoverNestedHelpersAndFailureBranches() throws Exception {
        assertTrue(DefaultCoredeuxRedisDataAccessService.CollectionUtils.isEmpty((List<?>) null));
        assertTrue(DefaultCoredeuxRedisDataAccessService.CollectionUtils.isEmpty(List.of()));
        assertTrue(DefaultCoredeuxRedisDataAccessService.CollectionUtils.isEmpty((Map<?, ?>) null));
        assertTrue(DefaultCoredeuxRedisDataAccessService.CollectionUtils.isEmpty(Map.of()));

        HelperSource source = new HelperSource();
        HelperTarget target = new HelperTarget();
        DefaultCoredeuxRedisDataAccessService.BeanUtils.copyProperties(source, target);
        assertEquals("Alpha", target.name);
        assertEquals(10, target.age);

        Field inherited = DefaultCoredeuxRedisDataAccessService.ReflectionUtils.findField(ChildEntity.class, "parentValue");
        assertNotNull(inherited);
        DefaultCoredeuxRedisDataAccessService.ReflectionUtils.doWithFields(ChildEntity.class, field -> {
            DefaultCoredeuxRedisDataAccessService.ReflectionUtils.makeAccessible(field);
        });
        Field sourceNameField = HelperSource.class.getDeclaredField("name");
        DefaultCoredeuxRedisDataAccessService.ReflectionUtils.makeAccessible(sourceNameField);
        assertEquals("Alpha", DefaultCoredeuxRedisDataAccessService.ReflectionUtils.getField(sourceNameField, source));
        Field targetNameField = HelperTarget.class.getDeclaredField("name");
        DefaultCoredeuxRedisDataAccessService.ReflectionUtils.makeAccessible(targetNameField);
        DefaultCoredeuxRedisDataAccessService.ReflectionUtils.setField(targetNameField, target, "Updated");
        assertEquals("Updated", target.name);

        when(commands.get(anyString())).thenThrow(new IllegalStateException("boom"));
        assertThrows(CoredeuxDataAccessException.class, () -> dataAccessService.load("1", SampleRedisEntity.class));

        when(commands.keys(anyString())).thenThrow(new IllegalStateException("boom"));
        assertThrows(CoredeuxDataAccessException.class,
                () -> dataAccessService.loadAll(List.of(), SampleRedisEntity.class, 10, 1));

        when(commands.set(anyString(), anyString())).thenThrow(new IllegalStateException("boom"));
        assertThrows(CoredeuxDataAccessException.class, () -> dataAccessService.save(sample(null, "X", 1, "p")));
    }

    private void persistSamples() {
        save(sample(null, "Alpha", 10, "one"));
        save(sample(null, "Beta", 20, "two"));
        save(sample(null, "Gamma", 30, "three"));
    }

    private String save(SampleRedisEntity entity) {
        try {
            return dataAccessService.save(entity);
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    private SampleRedisEntity sample(String id, String name, Integer age, String payload) {
        SampleRedisEntity entity = new SampleRedisEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setAge(age);
        entity.setPayload(payload);
        return entity;
    }

    private String redisKey(String id) {
        return "SampleRedisEntity:" + id;
    }

    private static class HelperSource {
        private String name = "Alpha";
        private int age = 10;
        private static String ignored = "x";
    }

    private static class HelperTarget {
        private String name;
        private int age;
    }

    private static class ParentEntity {
        private String parentValue;
    }

    private static class ChildEntity extends ParentEntity {
        private String childValue;
    }
}
