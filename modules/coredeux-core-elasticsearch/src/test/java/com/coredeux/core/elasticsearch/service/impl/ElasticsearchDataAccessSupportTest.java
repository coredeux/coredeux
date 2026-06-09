package com.coredeux.core.elasticsearch.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(new FactoryId("abc"), service.convertIdentifier("abc", FactoryId.class));
        assertEquals(new ConstructorId("abc"), service.invokeStringFactory(ConstructorId.class, "abc"));

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

    @Test
    void shouldCoverQueryHelpersAndPredicateBranches() {
        assertNotNull(service.buildStructuredQuery(null));
        List<SearchParams> nullOnly = new java.util.ArrayList<>();
        nullOnly.add(null);
        assertNotNull(service.buildStructuredQuery(nullOnly));
        assertEquals("demo-SampleElasticsearchEntity", service.indexName(SampleElasticsearchEntity.class));
        assertTrue(service.isPagingEnabled(1, 1));
        assertFalse(service.isPagingEnabled(0, 0));
        assertEquals(List.of(), service.defaultResults(null));
        assertEquals("value", service.normalizeRequired(" value ", "message"));
        assertThrows(CoredeuxValidationException.class, () -> service.normalizeRequired(" ", "message"));
        assertNotNull(service.termQuery("flag", true));
        assertNotNull(service.termQuery("count", 1));
        assertNotNull(service.termQuery("count", 1L));
        assertNotNull(service.termQuery("count", (short) 1));
        assertNotNull(service.termQuery("count", (byte) 1));
        assertNotNull(service.termQuery("count", 1.0f));
        assertNotNull(service.termQuery("count", 1.0d));
        assertNotNull(service.termQuery("name", "Alpha"));
        assertNotNull(service.containsQuery("tags", List.of("one", "two")));
        assertNotNull(service.containsQuery("tags", "one"));
        assertNotNull(service.notContainsQuery("tags", List.of("one", "two")));
        assertNotNull(service.notContainsQuery("tags", "one"));
        assertNotNull(service.rangeQuery("age", 10, null, null, null));
        assertNotNull(service.extractQuerySource("{\"query\":{\"term\":{\"age\":10}}}"));
        assertEquals("{\"term\":{\"age\":10}}", service.extractJsonValue("{\"term\":{\"age\":10}}", 0));
        assertNotNull(service.resolveQuery("{\"query\":{\"term\":{\"age\":10}}}"));
        assertThrows(CoredeuxValidationException.class,
                () -> service.buildCriteria(param("name", "UNKNOWN", "x")));
    }

    @Test
    void shouldCoverIdentifierConversionJsonLiteralAndFactoryBranches() {
        assertEquals("abc", service.convertIdentifier("abc", String.class));
        assertEquals(1L, service.convertIdentifier("1", Long.class));
        assertEquals(1, service.convertIdentifier("1", int.class));
        assertEquals((short) 1, service.convertIdentifier("1", Short.class));
        assertEquals((byte) 1, service.convertIdentifier("1", byte.class));
        assertEquals(new BigInteger("1"), service.convertIdentifier("1", BigInteger.class));
        assertEquals(new BigDecimal("1.5"), service.convertIdentifier("1.5", BigDecimal.class));
        assertEquals(Boolean.TRUE, service.convertIdentifier("true", Boolean.class));
        assertEquals(TestEnum.ONE, service.convertIdentifier("ONE", TestEnum.class));
        assertEquals(ValueOfFactory.valueOf("x"), service.invokeStringFactory(ValueOfFactory.class, "x"));
        assertEquals(OfFactory.of("x"), service.invokeStringFactory(OfFactory.class, "x"));
        assertEquals(FromStringFactory.fromString("x"), service.invokeStringFactory(FromStringFactory.class, "x"));
        assertEquals(new ConstructorFactory("x"), service.invokeStringFactory(ConstructorFactory.class, "x"));
        assertEquals(null, service.invokeStringFactory(NoFactory.class, "x"));
        assertThrows(CoredeuxValidationException.class, () -> service.convertIdentifier("x", NoFactory.class));

        assertEquals("\"alpha\"", service.toJsonLiteral("alpha"));
        assertEquals("\"A\"", service.toJsonLiteral('A'));
        assertEquals("true", service.toJsonLiteral(true));
        assertEquals("1", service.toJsonLiteral(1));
        assertEquals("{\"name\":\"Alpha\"}", service.toJsonLiteral(Map.of("name", "Alpha")));
        assertEquals("[1,2]", service.toJsonLiteral(List.of(1, 2)));
        assertEquals("[1,2]", service.toJsonLiteral(new int[] { 1, 2 }));
        assertEquals("a\\\"b\\\\c\\n", service.escapeJson("a\"b\\c\n"));

        assertEquals(null, service.containsQuery("tags", List.of()));
        assertEquals(null, service.notContainsQuery("tags", List.of()));
        assertThrows(CoredeuxValidationException.class,
                () -> service.resolveQueryTemplate("{\"name\":{{missing}}}", Map.of("name", "Alpha")));
    }

    private SearchParams param(String field, String comparator, Object value) {
        return SearchParams.builder().field(field).comparator(comparator).value(value).build();
    }

    enum TestEnum {
        ONE
    }

    public record FactoryId(String value) {
        public static FactoryId of(String value) {
            return new FactoryId(value);
        }
    }

    public record ValueOfFactory(String value) {
        public static ValueOfFactory valueOf(String value) {
            return new ValueOfFactory(value);
        }
    }

    public record OfFactory(String value) {
        public static OfFactory of(String value) {
            return new OfFactory(value);
        }
    }

    public record FromStringFactory(String value) {
        public static FromStringFactory fromString(String value) {
            return new FromStringFactory(value);
        }
    }

    public record ConstructorFactory(String value) {
    }

    public static class NoFactory {
    }

    public record ConstructorId(String value) {
    }

    public static class IdentifierEntity {
        private String id;
    }

    public static class NoIdentifierEntity {
        private String id;
    }
}
