package com.coredeux.core.jdbc.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.jdbc.testentity.SampleJdbcEntity;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

class JdbcDataAccessSupportTest {

    private DefaultCoredeuxJdbcDataAccessService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:coredeux_jdbc_support_" + UUID.randomUUID() + ";MODE=PostgreSQL");
        DataSource ds = dataSource;
        service = new DefaultCoredeuxJdbcDataAccessService(new NamedParameterJdbcTemplate(ds), "demo");
    }

    @Test
    void shouldResolveMetadataNamesAndColumnHelpers() {
        DefaultCoredeuxJdbcDataAccessService.JdbcEntityMetadata metadata = service.metadata(SchemaEntity.class);

        assertEquals("demo.custom_table", metadata.qualifiedTableName());
        assertEquals("entity_id", metadata.idColumn());
        assertEquals("display_name", metadata.columnNameForField("name"));
        assertEquals("entity_id as id, display_name as name", service.selectList(metadata));
        assertEquals("entity_id, display_name", service.insertColumns(metadata));
        assertEquals(":display_name", service.insertParameters(metadata, false));
        assertEquals(":entity_id, :display_name", service.insertParameters(metadata, true));
        assertArrayEquals(new String[] { "display_name" }, service.insertColumnNames(metadata, false));
        assertEquals("display_name = :display_name", service.updateAssignments(metadata));
        assertEquals(" order by entity_id", service.orderByClause(metadata));
        assertEquals("demo.FallbackEntity", service.resolveTableName(FallbackEntity.class));
        assertEquals("plain", service.resolveColumnName(FallbackEntity.class.getDeclaredFields()[0]));
    }

    @Test
    void shouldBuildSearchQueriesForEveryComparatorBranch() {
        DefaultCoredeuxJdbcDataAccessService.JdbcEntityMetadata metadata = service.metadata(SchemaEntity.class);
        List<SearchParams> params = new java.util.ArrayList<>(List.of(
                param("name", "EQUALS", "Alpha"),
                param("name", "NOTEQUALS", "Alpha"),
                param("name", "STARTSWITH", "Al"),
                param("name", "ANYWHERECS", "lp"),
                param("name", "ANYWHERE", "lp"),
                param("id", "LESSTHANOREQUAL", 20),
                param("id", "LESSTHAN", 20),
                param("id", "GREATERTHANOREQUAL", 10),
                param("id", "GREATERTHAN", 10),
                param("name", "ISNULL", null),
                param("name", "ISNOTNULL", null),
                param("name", "ISEMPTY", null),
                param("name", "ISNOTEMPTY", null),
                param("name", "CONTAINS", "one"),
                param("name", "NOTCONTAINS", "one")));
        params.add(null);

        DefaultCoredeuxJdbcDataAccessService.SearchQuerySpec spec = service.buildSearchQuery(params, metadata);

        assertFalse(spec.whereClause().isBlank());
        assertEquals(11, spec.parameters().getValues().size());
        assertEquals("", service.buildSearchQuery(List.of(param("name", "EQUALS", null)), metadata).whereClause());
        assertThrows(CoredeuxValidationException.class,
                () -> service.buildSearchQuery(List.of(param("missing", "EQUALS", "x")), metadata));
        assertThrows(CoredeuxValidationException.class,
                () -> service.buildSearchQuery(List.of(param("name", "UNKNOWN", "x")), metadata));
    }

    @Test
    void shouldHandlePagingPaginationValidationAndWrapping() {
        assertEquals("select 1", service.normalizeQuery(" select 1; "));
        assertEquals(" limit :__limit offset :__offset", service.pagingClause(10, 2));
        assertEquals("", service.pagingClause(-1, -1));

        PaginationData paged = service.buildPagination(5, 2, 1, 2);
        assertEquals(2L, paged.getTotalPages());
        PaginationData unpaged = service.buildPagination(5, 2, -1, -1);
        assertEquals(null, unpaged.getTotalResults());

        assertEquals(List.of(), service.defaultResults(null));
        assertEquals("null", service.typeName(null));
        assertThrows(CoredeuxValidationException.class, () -> service.validateEntity(null, "save"));
        assertThrows(CoredeuxValidationException.class, () -> service.validateSearchType(null));
        assertThrows(CoredeuxValidationException.class, () -> service.normalizeRequired(" ", "blank"));

        CoredeuxDataAccessException existing = new CoredeuxDataAccessException("x");
        assertSame(existing, service.wrap("message", existing));
        assertThrows(CoredeuxValidationException.class,
                () -> service.wrap("message", new CoredeuxValidationException("x")));
    }

    @Test
    void shouldConvertIdentifiersAndRejectUnsupportedMetadata() throws Exception {
        UUID uuid = UUID.randomUUID();

        assertEquals("1", service.convertIdentifier("1", String.class));
        assertEquals(1L, service.convertIdentifier("1", Long.class));
        assertEquals(1, service.convertIdentifier("1", int.class));
        assertEquals((short) 1, service.convertIdentifier("1", Short.class));
        assertEquals((byte) 1, service.convertIdentifier("1", byte.class));
        assertEquals(new BigInteger("1"), service.convertIdentifier("1", BigInteger.class));
        assertEquals(new BigDecimal("1.5"), service.convertIdentifier("1.5", BigDecimal.class));
        assertEquals(Boolean.TRUE, service.convertIdentifier("true", boolean.class));
        assertEquals(uuid, service.convertIdentifier(uuid.toString(), UUID.class));
        assertEquals(TestEnum.ONE, service.convertIdentifier("ONE", TestEnum.class));
        assertEquals(FactoryId.of("abc"), service.convertIdentifier("abc", FactoryId.class));
        assertEquals(null, service.invokeStringFactory(ConstructorId.class, "abc"));

        assertThrows(CoredeuxValidationException.class, () -> service.convertIdentifier("x", NoFactoryId.class));
        assertThrows(CoredeuxValidationException.class, () -> service.metadata(NoIdentifierEntity.class));
        assertThrows(CoredeuxValidationException.class, () -> service.metadata(OnlyTransientEntity.class));

        Field id = service.findIdentifierField(SchemaEntity.class);
        assertNotNull(id);
        assertEquals("id", id.getName());
    }

    private SearchParams param(String field, String comparator, Object value) {
        return SearchParams.builder().field(field).comparator(comparator).value(value).build();
    }

    enum TestEnum {
        ONE
    }

    record FactoryId(String value) {
        public static FactoryId of(String value) {
            return new FactoryId(value);
        }
    }

    static class ConstructorId {
        ConstructorId(String value) {
        }
    }

    static class NoFactoryId {
    }

    @Table(name = "custom_table")
    static class SchemaEntity {
        @Id
        @Column(name = "entity_id")
        private Long id;

        @Column(name = "display_name")
        private String name;

        private static String staticField;
        private transient String transientField;
        @Transient
        private String ignored;
    }

    static class FallbackEntity {
        private String plain;
        private String id;
    }

    static class NoIdentifierEntity {
        private String name;
    }

    static class OnlyTransientEntity {
        @Transient
        private String ignored;

        private static String staticField;
    }
}
