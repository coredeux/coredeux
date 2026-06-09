package com.coredeux.core.redis.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

class RedisDataAccessSupportTest {

    private RedisCommands<String, String> commands;
    private DefaultCoredeuxRedisDataAccessService service;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        StatefulRedisConnection<String, String> connection = mock(StatefulRedisConnection.class);
        @SuppressWarnings("unchecked")
        RedisCommands<String, String> mockedCommands = mock(RedisCommands.class);
        commands = mockedCommands;
        when(connection.sync()).thenReturn(commands);
        when(commands.incr(anyString())).thenReturn(7L);
        service = new DefaultCoredeuxRedisDataAccessService(connection, "demo");
    }

    @Test
    void shouldMatchEveryComparatorBranch() {
        Entity entity = new Entity();
        entity.name = "Alpha";
        entity.age = 10;
        entity.tags = List.of("one", "two");
        entity.aliases = new String[] { "A", "B" };
        entity.details = new Details("nested");
        entity.nullable = null;

        assertTrue(service.matches(entity, param("name", "EQUALS", "Alpha")));
        assertTrue(service.matches(entity, param("age", "EQUALS", 10L)));
        assertTrue(service.matches(entity, param("name", "NOTEQUALS", "Beta")));
        assertTrue(service.matches(entity, param("name", "STARTSWITH", "Al")));
        assertTrue(service.matches(entity, param("name", "ANYWHERECS", "lp")));
        assertTrue(service.matches(entity, param("name", "ANYWHERE", "alpha")));
        assertTrue(service.matches(entity, param("age", "LESSTHANOREQUAL", 10)));
        assertTrue(service.matches(entity, param("age", "LESSTHAN", 11)));
        assertTrue(service.matches(entity, param("age", "GREATERTHANOREQUAL", 10)));
        assertTrue(service.matches(entity, param("age", "GREATERTHAN", 9)));
        assertTrue(service.matches(entity, param("nullable", "ISNULL", null)));
        assertTrue(service.matches(entity, param("name", "ISNOTNULL", null)));
        assertFalse(service.matches(entity, param("name", "ISEMPTY", null)));
        assertTrue(service.matches(entity, param("tags", "ISNOTEMPTY", null)));
        assertTrue(service.matches(entity, param("tags", "CONTAINS", "one")));
        assertTrue(service.matches(entity, param("tags", "CONTAINS", List.of("one", "two"))));
        assertTrue(service.matches(entity, param("aliases", "CONTAINS", "A")));
        assertTrue(service.matches(entity, param("aliases", "CONTAINS", List.of("A", "B"))));
        assertTrue(service.matches(entity, param("name", "CONTAINS", "ph")));
        assertTrue(service.matches(entity, param("tags", "NOTCONTAINS", "three")));
        assertEquals("nested", service.resolveFieldValue(entity, "details.value"));

        assertThrows(CoredeuxValidationException.class, () -> service.matches(entity, param("name", "UNKNOWN", "x")));
        assertThrows(CoredeuxValidationException.class, () -> service.resolveFieldValue(entity, "details.missing"));
        assertThrows(CoredeuxValidationException.class, () -> service.compare(null, 1));
    }

    @Test
    void shouldFilterPageParseQueriesAndHandleEmptyValues() {
        Entity alpha = new Entity("1", "Alpha", 10);
        Entity beta = new Entity("2", "Beta", 20);
        List<SearchParams> params = new java.util.ArrayList<>();
        params.add(param("age", "GREATERTHAN", 10));
        params.add(null);
        List<Entity> filtered = service.filterEntities(List.of(alpha, beta), params);

        assertEquals(List.of(beta), filtered);
        assertEquals(List.of(beta), service.page(List.of(alpha, beta), 1, 2));
        assertEquals(List.of(alpha, beta), service.page(List.of(alpha, beta), -1, -1));
        assertEquals(List.of(), service.defaultResults(null));
        assertTrue(service.isEmpty(null));
        assertTrue(service.isEmpty(" "));
        assertTrue(service.isEmpty(List.of()));
        assertTrue(service.isEmpty(Map.of()));
        assertTrue(service.isEmpty(new String[] {}));
        assertFalse(service.isEmpty("x"));

        assertEquals(1, service.parseQueryRequest("[{\"field\":\"name\",\"comparator\":\"EQUALS\",\"value\":\"Alpha\"}]")
                .filters().size());
        assertEquals(1, service.parseQueryRequest("{\"field\":\"name\",\"comparator\":\"EQUALS\",\"value\":\"Alpha\"}")
                .filters().size());
        assertEquals(1, service.parseQueryRequest("{\"filters\":[{\"field\":\"name\",\"comparator\":\"EQUALS\",\"value\":\"Alpha\"}]}")
                .filters().size());
        assertThrows(CoredeuxValidationException.class, () -> service.parseQueryRequest("{}"));
        assertThrows(CoredeuxValidationException.class, () -> service.parseQueryRequest("{"));
    }

    @Test
    void shouldConvertAndGenerateIdentifiers() throws Exception {
        UUID uuid = UUID.randomUUID();

        assertEquals("1", service.convertIdentifier("1", String.class));
        assertEquals(uuid, service.convertIdentifier(uuid.toString(), UUID.class));
        assertEquals(1L, service.convertIdentifier("1", Long.class));
        assertEquals(1, service.convertIdentifier("1", int.class));
        assertEquals((short) 1, service.convertIdentifier("1", Short.class));
        assertEquals((byte) 1, service.convertIdentifier("1", byte.class));
        assertEquals(new BigInteger("1"), service.convertIdentifier("1", BigInteger.class));
        assertEquals(new BigDecimal("1.5"), service.convertIdentifier("1.5", BigDecimal.class));
        assertEquals(Boolean.TRUE, service.convertIdentifier("true", boolean.class));
        assertEquals(TestEnum.ONE, service.convertIdentifier("ONE", TestEnum.class));
        assertEquals("abc", service.convertIdentifier("abc", FactoryId.class));
        assertEquals("abc", service.convertIdentifier("abc", NoFactoryId.class));

        assertNotNull(service.generateIdentifier(StringId.class, StringId.class.getDeclaredField("id")));
        assertNotNull(service.generateIdentifier(UuidId.class, UuidId.class.getDeclaredField("id")));
        assertEquals(7L, service.generateIdentifier(LongId.class, LongId.class.getDeclaredField("id")));
        assertEquals(7, service.generateIdentifier(IntegerId.class, IntegerId.class.getDeclaredField("id")));
        assertEquals((short) 7, service.generateIdentifier(ShortId.class, ShortId.class.getDeclaredField("id")));
        assertEquals((byte) 7, service.generateIdentifier(ByteId.class, ByteId.class.getDeclaredField("id")));
        assertEquals(BigInteger.valueOf(7), service.generateIdentifier(BigIntegerId.class, BigIntegerId.class.getDeclaredField("id")));
        assertEquals(BigDecimal.valueOf(7), service.generateIdentifier(BigDecimalId.class, BigDecimalId.class.getDeclaredField("id")));
        assertNotNull(service.generateIdentifier(NoFactoryId.class, NoFactoryId.class.getDeclaredField("id")));
    }

    @Test
    void shouldResolveTemplatesPaginationNamespacesAndWrapFailures() {
        assertEquals("demo:Entity", service.redisNamespace(Entity.class));
        assertEquals("demo:Entity:1", service.redisKey(Entity.class, "1"));
        assertEquals("demo:Entity:seq", service.redisSequenceKey(Entity.class));
        assertEquals("{\"age\":10}", service.resolveQueryTemplate("{\"age\":{{age}}}", Map.of("age", 10)));
        assertThrows(CoredeuxValidationException.class,
                () -> service.resolveQueryTemplate("{\"age\":{{missing}}}", Map.of("age", 10)));

        PaginationData paged = service.buildPagination(5, 2, 1, 2);
        assertEquals(2L, paged.getTotalPages());
        PaginationData unpaged = service.buildPagination(5, 2, -1, -1);
        assertEquals(null, unpaged.getTotalResults());

        CoredeuxDataAccessException existing = new CoredeuxDataAccessException("x");
        assertSame(existing, service.wrap("message", existing));
        assertThrows(CoredeuxValidationException.class,
                () -> service.wrap("message", new CoredeuxValidationException("x")));
    }

    @Test
    void shouldCoverReflectionAndComparisonHelpers() throws Exception {
        Entity entity = new Entity("1", "Alpha", 10);
        entity.tags = List.of("one", "two");
        entity.aliases = new String[] { "A", "B" };
        entity.details = new Details("nested");

        DefaultCoredeuxRedisDataAccessService emptyPrefixService =
                new DefaultCoredeuxRedisDataAccessService(mock(StatefulRedisConnection.class), "");

        assertEquals("Entity", emptyPrefixService.redisNamespace(Entity.class));
        assertEquals("Entity:1", emptyPrefixService.redisKey(Entity.class, "1"));
        assertEquals("Entity:seq", emptyPrefixService.redisSequenceKey(Entity.class));
        assertEquals("nested", emptyPrefixService.resolveFieldValue(entity, "details.value"));
        assertTrue(emptyPrefixService.equalsValue(10, 10L));
        assertTrue(emptyPrefixService.contains(entity.aliases, "A"));
        assertTrue(emptyPrefixService.contains(entity.tags, List.of("one", "two")));
        assertFalse(emptyPrefixService.isEmpty(entity.tags));
        assertTrue(emptyPrefixService.isEmpty(List.of()));
        assertEquals(0, emptyPrefixService.compareNumbers(10, 10L));
        assertTrue(emptyPrefixService.compare(11, 10) > 0);
        assertEquals(List.of(), emptyPrefixService.page(List.of(), 10, 1));
        assertThrows(CoredeuxValidationException.class, () -> emptyPrefixService.compare(null, 1));
        assertThrows(CoredeuxValidationException.class, () -> emptyPrefixService.resolveFieldValue(entity, "details.missing"));
    }

    private SearchParams param(String field, String comparator, Object value) {
        return SearchParams.builder().field(field).comparator(comparator).value(value).build();
    }

    enum TestEnum {
        ONE
    }

    static class Entity {
        private String id;
        private String name;
        private Integer age;
        private List<String> tags;
        private String[] aliases;
        private Details details;
        private String nullable;

        Entity() {
        }

        Entity(String id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    record Details(String value) {
    }

    record FactoryId(String value) {
        static FactoryId of(String value) {
            return new FactoryId(value);
        }
    }

    static class NoFactoryId {
        private NoFactoryId id;
    }

    static class StringId {
        private String id;
    }

    static class UuidId {
        private UUID id;
    }

    static class LongId {
        private Long id;
    }

    static class IntegerId {
        private Integer id;
    }

    static class ShortId {
        private Short id;
    }

    static class ByteId {
        private Byte id;
    }

    static class BigIntegerId {
        private BigInteger id;
    }

    static class BigDecimalId {
        private BigDecimal id;
    }
}
