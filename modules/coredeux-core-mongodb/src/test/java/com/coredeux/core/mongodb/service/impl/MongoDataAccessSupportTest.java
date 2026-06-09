package com.coredeux.core.mongodb.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.mongodb.testentity.SampleMongoEntity;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

class MongoDataAccessSupportTest {

    private DefaultCoredeuxMongoDataAccessService service;

    @BeforeEach
    void setUp() {
        MongoDatabase database = mock(MongoDatabase.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        service = new DefaultCoredeuxMongoDataAccessService(database);
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
        Bson filter = service.buildSearchFilter(params);
        assertNotNull(filter);
        assertThrows(CoredeuxValidationException.class, () -> service.buildCriteria(param("name", "UNKNOWN", "x")));
    }

    @Test
    void shouldIgnoreNullValuedCriteriaAndBuildPaginationBranches() {
        assertNotNull(service.buildSearchFilter(List.of(
                param("name", "EQUALS", null),
                param("tags", "CONTAINS", null))));

        PaginationData unpaged = service.buildPagination(5, 2, -1, -1);
        assertEquals(-1L, unpaged.getCurrentPage());
        assertEquals(null, unpaged.getTotalResults());

        PaginationData paged = service.buildPagination(5, 2, 5, 2);
        assertEquals(2L, paged.getCurrentPage());
        assertEquals(5L, paged.getPageSize());
        assertEquals(5L, paged.getTotalResults());
        assertEquals(2L, paged.getResultSize());
    }

    @Test
    void shouldConvertIdentifiersAndUseFactories() throws Exception {
        UUID uuid = UUID.randomUUID();

        assertEquals("1", service.convertIdentifier("1", String.class));
        assertEquals(1L, service.convertIdentifier("1", Long.class));
        assertEquals(1, service.convertIdentifier("1", int.class));
        assertEquals((short) 1, service.convertIdentifier("1", Short.class));
        assertEquals((byte) 1, service.convertIdentifier("1", byte.class));
        assertEquals(new BigInteger("1"), service.convertIdentifier("1", BigInteger.class));
        assertEquals(new BigDecimal("1.5"), service.convertIdentifier("1.5", BigDecimal.class));
        assertEquals(Boolean.TRUE, service.convertIdentifier("true", Boolean.class));
        assertEquals(uuid, service.convertIdentifier(uuid.toString(), UUID.class));
        assertEquals(TestEnum.ONE, service.convertIdentifier("ONE", TestEnum.class));
        assertThrows(CoredeuxValidationException.class, () -> service.convertIdentifier("abc", FactoryId.class));
        assertEquals(null, service.invokeStringFactory(ConstructorId.class, "abc"));

        assertEquals("id", service.identifierField(IdentifierEntity.class).getName());
        assertThrows(CoredeuxValidationException.class, () -> service.identifierField(NoIdentifierEntity.class));
    }

    @Test
    void shouldResolveQueryTemplatesAndWrapFailures() {
        String resolved = service.resolveQueryTemplate("{\"age\": {{ age }}, \"name\": {{ name }}}",
                Map.of("age", 10, "name", "Alpha"));

        assertEquals("{\"age\": 10, \"name\": \"Alpha\"}", resolved);
        assertEquals("{\"query\":{\"term\":{\"age\":10}}}",
                service.resolveQueryTemplate("{\"query\":{\"term\":{\"age\":{{age}}}}}", Map.of("age", 10)));
        assertThrows(CoredeuxValidationException.class,
                () -> service.resolveQueryTemplate("{\"age\": {{missing}}}", Map.of("age", 10)));

        CoredeuxDataAccessException existing = new CoredeuxDataAccessException("x");
        assertSame(existing, service.wrap("message", existing));
        assertThrows(CoredeuxValidationException.class,
                () -> service.wrap("message", new CoredeuxValidationException("x")));
    }

    @Test
    void shouldCoverDocumentAndReflectionHelpers() throws Exception {
        SampleMongoEntity entity = new SampleMongoEntity();
        entity.setId("1");
        entity.setName("Alpha");
        entity.setAge(10);
        entity.setTags(List.of("one", "two"));

        assertEquals("sample_mongo_entities", service.collectionName(SampleMongoEntity.class));
        assertEquals("sample_mongo_entities", service.mongoDocumentName(SampleMongoEntity.class));
        assertTrue(service.fieldHasAnnotation(SampleMongoEntity.class.getDeclaredField("id"),
                "org.springframework.data.annotation.Id"));
        assertFalse(service.fieldHasAnnotation(SampleMongoEntity.class.getDeclaredField("name"),
                "org.springframework.data.annotation.Id"));
        SampleMongoEntity copy = new SampleMongoEntity();
        service.copyProperties(entity, copy);
        assertEquals("Alpha", copy.getName());
        assertNotNull(service.buildSearchFilter(null));
        assertNotNull(service.buildSearchFilter(List.of()));
        assertEquals("1", service.normalizeDocumentId(new Document("_id", "1"), SampleMongoEntity.class).get("id"));
        assertNotNull(service.normalizeDocument(new Document("id", "1").append("name", "Alpha")));
        assertNotNull(service.generateIdentifier(SampleMongoEntity.class, SampleMongoEntity.class.getDeclaredField("id")));

        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, service.convertIdentifier(uuid.toString(), UUID.class));
        assertEquals(new BigInteger("1"), service.convertIdentifier("1", BigInteger.class));
        assertEquals(new BigDecimal("1.5"), service.convertIdentifier("1.5", BigDecimal.class));
        assertEquals(Boolean.TRUE, service.convertIdentifier("true", boolean.class));
        assertTrue(service.resolveQueryTemplate("{\"name\": {{name}}}", Map.of("name", "Alpha")).contains("Alpha"));
        assertThrows(CoredeuxValidationException.class,
                () -> service.resolveQueryTemplate("{\"name\": {{missing}}}", Map.of("name", "Alpha")));
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

    static class IdentifierEntity {
        private String id;
    }

    static class NoIdentifierEntity {
        private String name;
    }
}
