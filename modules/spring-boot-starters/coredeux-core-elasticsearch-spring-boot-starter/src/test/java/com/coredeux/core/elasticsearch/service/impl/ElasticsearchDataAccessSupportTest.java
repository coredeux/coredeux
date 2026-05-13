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
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;

import com.coredeux.core.elasticsearch.testentity.SampleElasticsearchEntity;
import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;

class ElasticsearchDataAccessSupportTest {

    private ElasticsearchOperations operations;
    private DefaultCoredeuxElasticsearchDataAccessService service;

    @BeforeEach
    void setUp() {
        operations = mock(ElasticsearchOperations.class);
        service = new DefaultCoredeuxElasticsearchDataAccessService(operations, "demo");
    }

    @Test
    void shouldBuildFallbackIndexCoordinatesAndMatchAllQueries() {
        when(operations.getIndexCoordinatesFor(SampleElasticsearchEntity.class)).thenReturn(null);

        assertEquals("demo-SampleElasticsearchEntity",
                service.indexCoordinates(SampleElasticsearchEntity.class).getIndexName());
        assertTrue(service.buildStructuredQuery(null) instanceof StringQuery);
        List<SearchParams> params = new java.util.ArrayList<>();
        params.add(null);
        assertTrue(service.buildStructuredQuery(params) instanceof StringQuery);
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
        assertTrue(service.buildStructuredQuery(params) instanceof org.springframework.data.elasticsearch.core.query.CriteriaQuery);
        assertThrows(CoredeuxValidationException.class, () -> service.buildCriteria(param("name", "UNKNOWN", "x")));
    }

    @Test
    void shouldIgnoreNullValuedCriteriaAndApplyPaginationBranches() {
        assertEquals("{\"match_all\":{}}", ((StringQuery) service.buildStructuredQuery(List.of(
                param("name", "EQUALS", null),
                param("tags", "CONTAINS", null)))).getSource());

        Query paged = new StringQuery("{}");
        service.applyPaging(paged, 5, 0);
        service.applyPaging(paged, 5, 2);
        assertEquals(1, paged.getPageable().getPageNumber());

        PaginationData unpaged = service.buildPagination(5, 2, -1, -1);
        assertEquals(-1L, unpaged.getCurrentPage());
        assertEquals(null, unpaged.getTotalResults());
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

        Field field = service.identifierField(AnnotatedIdentifierEntity.class);
        assertEquals("identifier", field.getName());
        assertThrows(CoredeuxValidationException.class, () -> service.identifierField(NoIdentifierEntity.class));
    }

    @Test
    void shouldResolveQueryTemplatesAndWrapFailures() {
        String resolved = service.resolveQueryTemplate("{\"age\":{{age}},\"name\":{{name}}}",
                Map.of("age", 10, "name", "Alpha"));

        assertEquals("{\"age\":10,\"name\":\"Alpha\"}", resolved);
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

    static class AnnotatedIdentifierEntity {
        @Id
        private String identifier;
    }

    static class NoIdentifierEntity {
        private String name;
    }
}
