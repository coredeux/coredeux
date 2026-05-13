package com.coredeux.core.jpa.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.jpa.testentity.SampleJpaEntity;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

class PostgresCoredeuxJpaDataAccessServiceTest {

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private PostgresCoredeuxJpaDataAccessService dataAccessService;

    @BeforeAll
    static void setUpFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory("coredeux-core-jpa-test");
    }

    @AfterAll
    static void tearDownFactory() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        entityManager = entityManagerFactory.createEntityManager();
        clearDatabase();
        dataAccessService = new PostgresCoredeuxJpaDataAccessService();
        dataAccessService.setEntityManager(entityManager);
    }

    @Test
    void shouldFallbackToGenericJpaForNonJsonComparators() {
        persistSamples();

        SearchResult<SampleJpaEntity> result = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("EQUALS").value("Alpha").build()),
                SampleJpaEntity.class, 10, 1);

        assertEquals(1, result.getResults().size());
        assertFalse(dataAccessService.containsJsonComparators(List.of(
                SearchParams.builder().field("name").comparator("EQUALS").value("Alpha").build())));
    }

    @Test
    void shouldDetectJsonComparatorsAndBuildNativeSpec() {
        PostgresCoredeuxJpaDataAccessService.NativeQuerySpec spec = dataAccessService.buildNativeQuerySpec(List.of(
                SearchParams.builder().field("name").comparator("EQUALS").value("Alpha").build(),
                SearchParams.builder().field("payload.customer.name").comparator("jsonb(text)")
                        .value(Map.of("operator", "ANYWHERE", "value", "john")).build()), SampleJpaEntity.class);

        assertTrue(dataAccessService.containsJsonComparators(List.of(
                SearchParams.builder().field("payload.customer.name").comparator("jsonb(text)").value("= ''").build())));
        assertEquals("sample_jpa_entity", spec.tableName);
        assertTrue(spec.whereClause.contains("entity_alias.name = ?"));
        assertTrue(spec.whereClause.contains("lower(entity_alias.json_payload #>> '{customer,name}') like ?"));
        assertEquals(2, spec.parameters.size());
        assertEquals("Alpha", spec.parameters.get(0));
        assertEquals("%john%", spec.parameters.get(1));
    }

    @Test
    void shouldBuildLegacyJsonbConditions() {
        String textCondition = dataAccessService.buildJsonTextCondition(SampleJpaEntity.class,
                SearchParams.builder().field("payload->>'name'").comparator("jsonb(text)").value("= 'alpha'").build(),
                new java.util.ArrayList<>());
        String numericCondition = dataAccessService.buildJsonNumericCondition(SampleJpaEntity.class,
                SearchParams.builder().field("payload.metrics.age").comparator("jsonb(numeric)")
                        .value("cast(%s as numeric) > 18").build(),
                new java.util.ArrayList<>());

        assertEquals("entity_alias.json_payload->>'name' = 'alpha'", textCondition);
        assertEquals("cast(entity_alias.json_payload #>> '{metrics,age}' as numeric) > 18", numericCondition);
    }

    @Test
    void shouldResolveTableAndColumnNamesFromAnnotations() {
        assertEquals("sample_jpa_entity", dataAccessService.resolveTableName(SampleJpaEntity.class));
        assertEquals("json_payload", dataAccessService.resolveColumnName(SampleJpaEntity.class, "payload"));
        assertEquals("name", dataAccessService.resolveColumnName(SampleJpaEntity.class, "name"));
    }

    @Test
    void shouldFailForInvalidJsonbConfiguration() {
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.buildJsonTextCondition(SampleJpaEntity.class,
                        SearchParams.builder().field("payload.customer.name").comparator("jsonb(text)").value(Map.of())
                                .build(),
                        new java.util.ArrayList<>()));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.buildJsonNumericCondition(SampleJpaEntity.class,
                        SearchParams.builder().field("payload.customer.age").comparator("jsonb(numeric)").value(Map.of())
                                .build(),
                        new java.util.ArrayList<>()));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.buildNativeStandardCondition(SampleJpaEntity.class,
                        SearchParams.builder().field("tags").comparator("CONTAINS").value("common").build(),
                        new java.util.ArrayList<>()));
    }

    private void persistSamples() {
        entityManager.getTransaction().begin();
        entityManager.persist(sample("Alpha", 10, "{}", "one"));
        entityManager.persist(sample("Beta", 20, "{}"));
        entityManager.getTransaction().commit();
        entityManager.clear();
    }

    private void clearDatabase() {
        entityManager.getTransaction().begin();
        entityManager.createQuery("delete from SampleJpaEntity").executeUpdate();
        entityManager.getTransaction().commit();
        entityManager.clear();
    }

    private SampleJpaEntity sample(String name, Integer age, String payload, String... tags) {
        SampleJpaEntity entity = new SampleJpaEntity();
        entity.setName(name);
        entity.setAge(age);
        entity.setPayload(payload);
        entity.setTags(tags == null ? List.of() : List.of(tags));
        return entity;
    }
}
