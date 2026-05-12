package com.coredeux.core.elasticsearch.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import com.coredeux.core.elasticsearch.testentity.SampleElasticsearchEntity;
import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;

class ElasticsearchDataAccessSupportTest {

    private ElasticsearchGateway gateway;
    private DefaultCoredeuxElasticsearchDataAccessService service;

    @BeforeEach
    void setUp() {
        gateway = mock(ElasticsearchGateway.class);
        service = new DefaultCoredeuxElasticsearchDataAccessService(gateway, "demo");
    }

    @Test
    void shouldBuildFallbackIndexNameAndMatchAllQueries() {
        assertEquals("demo-SampleElasticsearchEntity", service.indexName(SampleElasticsearchEntity.class));
        assertNotNull(service.buildStructuredQuery(null));
        List<SearchParams> params = new java.util.ArrayList<>();
        params.add(null);
        assertNotNull(service.buildStructuredQuery(params));
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
                param("tags", "CONTAINS", List.of("one", "two")),
                param("tags", "CONTAINS", "one"),
                param("tags", "NOTCONTAINS", List.of("one", "two")),
                param("tags", "NOTCONTAINS", "one"));

        for (SearchParams param : params) {
            assertNotNull(service.buildCriteria(param));
        }
        assertNotNull(service.buildStructuredQuery(params));
        assertThrows(CoredeuxValidationException.class, () -> service.buildCriteria(param("name", "UNKNOWN", "x")));
    }

    @Test
    void shouldIgnoreNullValuedCriteriaAndBuildPaginationBranches() {
        assertNotNull(service.buildStructuredQuery(List.of(
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

        Field field = service.identifierField(IdentifierEntity.class);
        assertEquals("id", field.getName());
        assertEquals("id", service.identifierField(NoIdentifierEntity.class).getName());
    }

    @Test
    void shouldResolveQueryTemplatesAndWrapFailures() {
        String resolved = service.resolveQueryTemplate("{\"age\":{{age}},\"name\":{{name}}}",
                Map.of("age", 10, "name", "Alpha"));

        assertEquals("{\"age\":10,\"name\":\"Alpha\"}", resolved);
        Query query = service.resolveQuery("{\"query\":{\"term\":{\"age\":10}}}");
        assertNotNull(query);
        assertNotNull(query);
        assertThrows(CoredeuxValidationException.class,
                () -> service.resolveQueryTemplate("{\"age\":{{age}}}", Map.of("other", 1)));

        CoredeuxDataAccessException existing = new CoredeuxDataAccessException("x");
        assertSame(existing, service.wrap("message", existing));
        assertThrows(CoredeuxValidationException.class,
                () -> service.wrap("message", new CoredeuxValidationException("x")));
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
        private String id;
    }
}
