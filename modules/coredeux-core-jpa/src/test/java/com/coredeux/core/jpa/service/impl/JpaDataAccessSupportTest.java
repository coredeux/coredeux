package com.coredeux.core.jpa.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.jpa.testentity.SampleJpaEntity;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;

class JpaDataAccessSupportTest {

    private final DefaultCoredeuxJpaDataAccessService jpa = new DefaultCoredeuxJpaDataAccessService();
    private final PostgresCoredeuxJpaDataAccessService postgres = new PostgresCoredeuxJpaDataAccessService();

    @Test
    void shouldHandleGenericJpaSupportBranches() {
        assertTrue(jpa.supportedComparators(SampleJpaEntity.class).contains("CONTAINS"));
        assertEquals(List.of(), jpa.defaultResults(null));
        assertEquals(2L, jpa.buildPagination(5, 2, 1, 2).getTotalPages());
        PaginationData unpaged = jpa.buildPagination(5, 2, -1, -1);
        assertEquals(null, unpaged.getTotalResults());

        assertThrows(CoredeuxValidationException.class, () -> jpa.validateEntity(null, "save"));
        assertThrows(CoredeuxValidationException.class, () -> jpa.validateSearchType(null));
        assertThrows(CoredeuxValidationException.class, () -> jpa.normalizeRequired(" ", "blank"));
        CoredeuxDataAccessException existing = new CoredeuxDataAccessException("x");
        assertSame(existing, jpa.wrap("message", existing));
        assertThrows(CoredeuxValidationException.class,
                () -> jpa.wrap("message", new CoredeuxValidationException("x")));
    }

    @Test
    void shouldBuildPostgresJsonAndStandardConditions() {
        List<Object> parameters = new ArrayList<>();

        List<SearchParams> nonJsonParams = new ArrayList<>();
        nonJsonParams.add(null);
        nonJsonParams.add(SearchParams.builder().field("name").comparator(null).build());
        assertFalse(postgres.containsJsonComparators(nonJsonParams));
        assertTrue(postgres.supportedComparators(SampleJpaEntity.class)
                .contains(PostgresCoredeuxJpaDataAccessService.JSONB_NUMERIC));
        assertEquals("UnannotatedEntity", postgres.resolveTableName(UnannotatedEntity.class));

        assertEquals("entity_alias.json_payload::text",
                postgres.buildJsonTextExpression(SampleJpaEntity.class, "payload"));
        assertEquals("entity_alias.json_payload#>'name'",
                postgres.buildJsonTextExpression(SampleJpaEntity.class, "payload#>'name'"));

        assertEquals("entity_alias.name <> ?", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "NOTEQUALS", "Alpha"), parameters));
        assertEquals("entity_alias.name like ?", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "STARTSWITH", "Al"), parameters));
        assertEquals("entity_alias.name like ?", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "ANYWHERECS", "lp"), parameters));
        assertEquals("lower(entity_alias.name) like ?", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "ANYWHERE", "lp"), parameters));
        assertEquals("entity_alias.age < ?", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("age", "LESSTHAN", 20), parameters));
        assertEquals("entity_alias.age <= ?", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("age", "LESSTHANOREQUAL", 20), parameters));
        assertEquals("entity_alias.age > ?", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("age", "GREATERTHAN", 10), parameters));
        assertEquals("entity_alias.age >= ?", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("age", "GREATERTHANOREQUAL", 10), parameters));
        assertEquals("entity_alias.name is null", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "ISNULL", null), parameters));
        assertEquals("entity_alias.name is not null", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "ISNOTNULL", null), parameters));
        assertEquals("1 = 1", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "EQUALS", null), parameters));
        assertEquals("1 = 1", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "NOTEQUALS", null), parameters));
        assertEquals("1 = 1", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "STARTSWITH", null), parameters));
        assertEquals("1 = 1", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "ANYWHERECS", null), parameters));
        assertEquals("1 = 1", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("name", "ANYWHERE", null), parameters));
        assertEquals("1 = 1", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("age", "LESSTHAN", null), parameters));
        assertEquals("1 = 1", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("age", "LESSTHANOREQUAL", null), parameters));
        assertEquals("1 = 1", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("age", "GREATERTHAN", null), parameters));
        assertEquals("1 = 1", postgres.buildNativeStandardCondition(SampleJpaEntity.class,
                param("age", "GREATERTHANOREQUAL", null), parameters));
        assertEquals("", postgres.buildNativeQuerySpec(List.of(), SampleJpaEntity.class).whereClause);
        List<SearchParams> paramsWithNull = new ArrayList<>();
        paramsWithNull.add(null);
        paramsWithNull.add(param("name", "EQUALS", "Alpha"));
        assertEquals(" where entity_alias.name = ?",
                postgres.buildNativeQuerySpec(paramsWithNull, SampleJpaEntity.class).whereClause);
        assertThrows(CoredeuxValidationException.class,
                () -> postgres.resolveColumnName(SampleJpaEntity.class, "missing"));
        assertThrows(CoredeuxValidationException.class,
                () -> postgres.buildNativeStandardCondition(SampleJpaEntity.class, param("name", "CONTAINS", "x"), parameters));
    }

    @Test
    void shouldBuildJsonOperatorBranchesAndFailures() {
        List<Object> parameters = new ArrayList<>();

        assertEquals("entity_alias.json_payload #>> '{name}' = ?",
                postgres.buildJsonTextCondition(SampleJpaEntity.class,
                        param("payload.name", "jsonb(text)", Map.of("operator", "EQUALS", "value", "Alpha")),
                        parameters));
        assertEquals("entity_alias.json_payload #>> '{name}' <> ?",
                postgres.buildJsonTextCondition(SampleJpaEntity.class,
                        param("payload.name", "jsonb(text)", Map.of("operator", "NOTEQUALS", "value", "Alpha")),
                        parameters));
        assertEquals("entity_alias.json_payload #>> '{name}' like ?",
                postgres.buildJsonTextCondition(SampleJpaEntity.class,
                        param("payload.name", "jsonb(text)", Map.of("operator", "STARTSWITH", "value", "Al")),
                        parameters));
        assertEquals("entity_alias.json_payload #>> '{name}' like ?",
                postgres.buildJsonTextCondition(SampleJpaEntity.class,
                        param("payload.name", "jsonb(text)", Map.of("operator", "ANYWHERECS", "value", "lp")),
                        parameters));
        assertEquals("lower(entity_alias.json_payload #>> '{name}') like ?",
                postgres.buildJsonTextCondition(SampleJpaEntity.class,
                        param("payload.name", "jsonb(text)", Map.of("operator", "ANYWHERE", "value", "LP")),
                        parameters));
        assertEquals("entity_alias.json_payload #>> '{name}' is not null",
                postgres.buildJsonTextCondition(SampleJpaEntity.class,
                        param("payload.name", "jsonb(text)", "is not null"), parameters));

        assertEquals("cast(entity_alias.json_payload #>> '{age}' as numeric) = ?",
                postgres.buildJsonNumericCondition(SampleJpaEntity.class,
                        param("payload.age", "jsonb(numeric)", Map.of("operator", "EQUALS", "value", 10)),
                        parameters));
        assertEquals("cast(entity_alias.json_payload #>> '{age}' as numeric) <> ?",
                postgres.buildJsonNumericCondition(SampleJpaEntity.class,
                        param("payload.age", "jsonb(numeric)", Map.of("operator", "NOTEQUALS", "value", 10)),
                        parameters));
        assertEquals("cast(entity_alias.json_payload #>> '{age}' as numeric) < ?",
                postgres.buildJsonNumericCondition(SampleJpaEntity.class,
                        param("payload.age", "jsonb(numeric)", Map.of("operator", "LESSTHAN", "value", 10)),
                        parameters));
        assertEquals("cast(entity_alias.json_payload #>> '{age}' as numeric) <= ?",
                postgres.buildJsonNumericCondition(SampleJpaEntity.class,
                        param("payload.age", "jsonb(numeric)", Map.of("operator", "LESSTHANOREQUAL", "value", 10)),
                        parameters));
        assertEquals("cast(entity_alias.json_payload #>> '{age}' as numeric) > ?",
                postgres.buildJsonNumericCondition(SampleJpaEntity.class,
                        param("payload.age", "jsonb(numeric)", Map.of("operator", "GREATERTHAN", "value", 10)),
                        parameters));
        assertEquals("cast(entity_alias.json_payload #>> '{age}' as numeric) >= ?",
                postgres.buildJsonNumericCondition(SampleJpaEntity.class,
                        param("payload.age", "jsonb(numeric)", Map.of("operator", "GREATERTHANOREQUAL", "value", 10)),
                        parameters));
        assertEquals("cast(entity_alias.json_payload #>> '{age}' as numeric) > 0",
                postgres.buildJsonNumericCondition(SampleJpaEntity.class,
                        param("payload.age", "jsonb(numeric)", "cast(%s as numeric) > 0"), parameters));

        assertThrows(CoredeuxValidationException.class,
                () -> postgres.buildJsonTextCondition(SampleJpaEntity.class,
                        param("payload.name", "jsonb(text)", Map.of("operator", "CONTAINS", "value", "x")),
                        new ArrayList<>()));
        assertThrows(CoredeuxValidationException.class,
                () -> postgres.buildJsonNumericCondition(SampleJpaEntity.class,
                        param("payload.age", "jsonb(numeric)", Map.of("operator", "ANYWHERE", "value", "x")),
                        new ArrayList<>()));
        assertThrows(CoredeuxValidationException.class,
                () -> postgres.buildJsonTextCondition(SampleJpaEntity.class,
                        param("payload.name", "jsonb(text)", " "), new ArrayList<>()));
        assertThrows(CoredeuxValidationException.class,
                () -> postgres.buildJsonNumericCondition(SampleJpaEntity.class,
                        param("payload.age", "jsonb(numeric)", " "), new ArrayList<>()));
    }

    private SearchParams param(String field, String comparator, Object value) {
        return SearchParams.builder().field(field).comparator(comparator).value(value).build();
    }

    static class UnannotatedEntity {
    }
}
