package com.coredeux.core.mongodb.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.mongodb.testentity.SampleMongoEntity;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;

class MongoDataAccessSupportTest {

    private DefaultCoredeuxMongoDataAccessService service;

    @BeforeEach
    void setUp() {
        service = new DefaultCoredeuxMongoDataAccessService(mock(MongoTemplate.class));
    }

    @Test
    void shouldBuildCriteriaForEveryComparatorBranch() {
        List<SearchParams> params = List.of(
                param("name", "EQUALS", "Alpha"),
                param("name", "NOTEQUALS", "Alpha"),
                param("name", "STARTSWITH", "Al"),
                param("name", "ANYWHERECS", "lp"),
                param("name", "ANYWHERE", "lp"),
                param("age", "LESSTHANOREQUAL", 20),
                param("age", "LESSTHAN", 20),
                param("age", "GREATERTHANOREQUAL", 10),
                param("age", "GREATERTHAN", 10),
                param("name", "ISNULL", null),
                param("name", "ISNOTNULL", null),
                param("tags", "ISEMPTY", null),
                param("tags", "ISNOTEMPTY", null),
                param("tags", "CONTAINS", "one"),
                param("tags", "NOTCONTAINS", "one"));

        for (SearchParams param : params) {
            assertNotNull(service.buildCriteria(param));
        }
        Query query = service.buildSearchQuery(params);
        assertFalse(query.getQueryObject().isEmpty());
        assertThrows(CoredeuxValidationException.class, () -> service.buildCriteria(param("name", "UNKNOWN", "x")));
    }

    @Test
    void shouldIgnoreNullValuedCriteriaAndApplyPagingBranches() {
        assertEquals("{}", service.buildSearchQuery(List.of(
                param("name", "EQUALS", null),
                param("tags", "CONTAINS", null))).getQueryObject().toJson());

        Query query = new Query();
        service.applyPaging(query, 5, 0);
        service.applyPaging(query, 5, 2);
        assertEquals(5, query.getSkip());

        PaginationData unpaged = service.buildPagination(5, 2, -1, -1);
        assertEquals(-1L, unpaged.getCurrentPage());
        assertEquals(null, unpaged.getTotalResults());
    }

    @Test
    void shouldConvertIdentifiersResolveTemplatesAndWrapFailures() {
        ObjectId objectId = new ObjectId();
        UUID uuid = UUID.randomUUID();

        assertEquals("1", service.convertIdentifier("1", String.class));
        assertEquals(objectId, service.convertIdentifier(objectId.toHexString(), ObjectId.class));
        assertEquals(uuid, service.convertIdentifier(uuid.toString(), UUID.class));
        assertEquals(1L, service.convertIdentifier("1", long.class));
        assertEquals(1, service.convertIdentifier("1", Integer.class));
        assertEquals((short) 1, service.convertIdentifier("1", short.class));
        assertEquals((byte) 1, service.convertIdentifier("1", Byte.class));
        assertEquals(new BigInteger("1"), service.convertIdentifier("1", BigInteger.class));
        assertEquals(new BigDecimal("1.5"), service.convertIdentifier("1.5", BigDecimal.class));
        assertEquals(Boolean.TRUE, service.convertIdentifier("true", boolean.class));
        assertEquals(TestEnum.ONE, service.convertIdentifier("ONE", TestEnum.class));
        assertThrows(CoredeuxValidationException.class, () -> service.convertIdentifier("abc", FactoryId.class));
        assertEquals(null, service.invokeStringFactory(ConstructorId.class, "abc"));

        assertEquals("{\"age\": 10}", service.resolveQueryTemplate("{\"age\": {{ age }}}", Map.of("age", 10)));
        assertThrows(CoredeuxValidationException.class,
                () -> service.resolveQueryTemplate("{\"age\": {{missing}}}", Map.of("age", 10)));

        CoredeuxDataAccessException existing = new CoredeuxDataAccessException("x");
        assertSame(existing, service.wrap("message", existing));
        assertThrows(CoredeuxValidationException.class,
                () -> service.wrap("message", new CoredeuxValidationException("x")));
    }

    @Test
    void shouldResolveIdentifierFieldsAndDefaults() {
        SampleMongoEntity entity = new SampleMongoEntity();
        entity.setId("id-1");

        assertEquals("id-1", service.extractIdentifier(entity));
        assertEquals(String.class, service.resolveIdentifierType(NoIdentifierEntity.class));
        assertEquals(List.of(), service.defaultResults(null));
        assertThrows(CoredeuxValidationException.class, () -> service.compare("left", new Object(),
                DefaultCoredeuxMongoDataAccessService.ComparisonType.LESS_THAN));
    }

    private SearchParams param(String field, String comparator, Object value) {
        return SearchParams.builder().field(field).comparator(comparator).value(value).build();
    }

    enum TestEnum {
        ONE
    }

    record FactoryId(String value) {
        static FactoryId of(String value) {
            return new FactoryId(value);
        }
    }

    record ConstructorId(String value) {
    }

    static class NoIdentifierEntity {
        private String name;
    }
}
