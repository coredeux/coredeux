package com.coredeux.core.jpa.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.jpa.testentity.SampleJpaEntity;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

class DefaultCoredeuxJpaDataAccessServiceTest {

    private static EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;
    private DefaultCoredeuxJpaDataAccessService dataAccessService;

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
        dataAccessService = new DefaultCoredeuxJpaDataAccessService();
        dataAccessService.setEntityManager(entityManager);
    }

    @Test
    void shouldSaveLoadUpdateRemoveAndRefreshEntity() {
        entityManager.getTransaction().begin();
        SampleJpaEntity created = sample("Alpha", 10, "one", "two");
        String id = dataAccessService.save(created);
        entityManager.getTransaction().commit();

        assertNotNull(id);
        assertNotNull(created.getId());

        SampleJpaEntity loaded = dataAccessService.load(id, SampleJpaEntity.class);
        assertEquals("Alpha", loaded.getName());
        assertFalse(entityManager.contains(loaded));

        SampleJpaEntity detachedUpdate = new SampleJpaEntity();
        detachedUpdate.setId(Long.valueOf(id));
        detachedUpdate.setName("Alpha Updated");
        detachedUpdate.setAge(15);
        detachedUpdate.setTags(List.of("two"));

        entityManager.getTransaction().begin();
        dataAccessService.update(detachedUpdate);
        entityManager.getTransaction().commit();

        SampleJpaEntity updated = entityManager.find(SampleJpaEntity.class, Long.valueOf(id));
        assertEquals("Alpha Updated", updated.getName());
        assertEquals(15, updated.getAge());

        entityManager.getTransaction().begin();
        SampleJpaEntity managed = entityManager.find(SampleJpaEntity.class, Long.valueOf(id));
        managed.setName("Mutated");
        dataAccessService.refresh(managed);
        entityManager.getTransaction().commit();

        assertEquals("Alpha Updated", managed.getName());

        entityManager.getTransaction().begin();
        dataAccessService.remove(detachedUpdate);
        entityManager.getTransaction().commit();

        assertNull(entityManager.find(SampleJpaEntity.class, Long.valueOf(id)));
    }

    @Test
    void shouldLoadAllUsingSupportedComparatorsAndPagination() {
        persistSamples();

        SearchResult<SampleJpaEntity> equalsResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("EQUALS").value("Alpha").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(1, equalsResult.getResults().size());
        assertEquals(3L, equalsResult.getPagination().getTotalResults());
        assertEquals(1L, equalsResult.getPagination().getResultSize());

        SearchResult<SampleJpaEntity> containsResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("tags").comparator("CONTAINS").value("common").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(2, containsResult.getResults().size());

        SearchResult<SampleJpaEntity> anywhereResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("ANYWHERE").value("ha").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(1, anywhereResult.getResults().size());

        SearchResult<SampleJpaEntity> notEqualsResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("NOTEQUALS").value("Alpha").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(2, notEqualsResult.getResults().size());

        SearchResult<SampleJpaEntity> startsWithResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("STARTSWITH").value("Al").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(1, startsWithResult.getResults().size());

        SearchResult<SampleJpaEntity> anywhereCaseSensitiveResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("ANYWHERECS").value("am").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(1, anywhereCaseSensitiveResult.getResults().size());

        SearchResult<SampleJpaEntity> rangeResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("age").comparator("GREATERTHAN").value(10).build()),
                SampleJpaEntity.class, 1, 1);
        assertEquals(1, rangeResult.getResults().size());
        assertEquals(2L, rangeResult.getPagination().getResultSize());
        assertEquals(2L, rangeResult.getPagination().getTotalPages());

        SearchResult<SampleJpaEntity> lessThanResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("age").comparator("LESSTHAN").value(20).build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(1, lessThanResult.getResults().size());

        SearchResult<SampleJpaEntity> lessThanOrEqualResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("age").comparator("LESSTHANOREQUAL").value(20).build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(2, lessThanOrEqualResult.getResults().size());

        SearchResult<SampleJpaEntity> greaterThanOrEqualResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("age").comparator("GREATERTHANOREQUAL").value(20).build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(2, greaterThanOrEqualResult.getResults().size());

        SearchResult<SampleJpaEntity> notContainsResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("tags").comparator("NOTCONTAINS").value("one").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(2, notContainsResult.getResults().size());

        SearchResult<SampleJpaEntity> emptyResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("tags").comparator("ISEMPTY").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(1, emptyResult.getResults().size());

        SearchResult<SampleJpaEntity> notEmptyResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("tags").comparator("ISNOTEMPTY").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(2, notEmptyResult.getResults().size());

        SearchResult<SampleJpaEntity> notNullResult = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("ISNOTNULL").build()),
                SampleJpaEntity.class, 10, 1);
        assertEquals(3, notNullResult.getResults().size());
    }

    @Test
    void shouldQueryWithNamedParametersAndPagination() {
        persistSamples();

        SearchResult<SampleJpaEntity> result = dataAccessService.query(
                "select e from SampleJpaEntity e where e.age >= :age order by e.name", Map.of("age", 10),
                SampleJpaEntity.class, 1, 1);

        assertEquals(1, result.getResults().size());
        assertEquals(3L, result.getPagination().getTotalResults());
        assertEquals(3L, result.getPagination().getResultSize());
        assertEquals(3L, result.getPagination().getTotalPages());
    }

    @Test
    void shouldSupportUnpagedOperations() {
        persistSamples();

        SearchResult<SampleJpaEntity> result = dataAccessService.loadAll(List.of(), SampleJpaEntity.class, -1, -1);

        assertEquals(3, result.getResults().size());
        assertEquals(-1L, result.getPagination().getCurrentPage());
        assertEquals(-1L, result.getPagination().getPageSize());
        assertNull(result.getPagination().getTotalResults());
    }

    @Test
    void shouldFailForInvalidSearchAndInvalidQueryInputs() {
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.load(null, SampleJpaEntity.class));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.load("1", null));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.loadAll(List.of(SearchParams.builder().field("name").comparator("UNKNOWN").value("x").build()),
                        SampleJpaEntity.class, 10, 1));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.loadAll(List.of(SearchParams.builder().field(" ").comparator("EQUALS").value("x").build()),
                        SampleJpaEntity.class, 10, 1));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.loadAll(List.of(SearchParams.builder().field("name").comparator("CONTAINS").value("x").build()),
                        SampleJpaEntity.class, 10, 1));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.loadAll(List.of(SearchParams.builder().field("age").comparator("GREATERTHAN").value(new Object()).build()),
                        SampleJpaEntity.class, 10, 1));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.query(" ", Map.of(), SampleJpaEntity.class, 10, 1));
    }

    @Test
    void shouldWrapJpaFailuresAsDataAccessExceptions() {
        assertThrows(CoredeuxDataAccessException.class,
                () -> dataAccessService.query("select missing from SampleJpaEntity", Map.of(), SampleJpaEntity.class, 10, 1));
        assertThrows(CoredeuxDataAccessException.class,
                () -> dataAccessService.load("not-a-number", SampleJpaEntity.class));
    }

    @Test
    void shouldAllowNullValuedComparatorsToBeIgnored() {
        persistSamples();

        SearchResult<SampleJpaEntity> result = assertDoesNotThrow(() -> dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("EQUALS").value(null).build()),
                SampleJpaEntity.class, 10, 1));

        assertEquals(3, result.getResults().size());
    }

    private void persistSamples() {
        entityManager.getTransaction().begin();
        entityManager.persist(sample("Alpha", 10, "common", "one"));
        entityManager.persist(sample("Beta", 20));
        entityManager.persist(sample("Gamma", 30, "common"));
        entityManager.getTransaction().commit();
        entityManager.clear();
    }

    private void clearDatabase() {
        entityManager.getTransaction().begin();
        entityManager.createQuery("delete from SampleJpaEntity").executeUpdate();
        entityManager.getTransaction().commit();
        entityManager.clear();
    }

    private SampleJpaEntity sample(String name, Integer age, String... tags) {
        SampleJpaEntity entity = new SampleJpaEntity();
        entity.setName(name);
        entity.setAge(age);
        entity.setTags(tags == null ? List.of() : List.of(tags));
        return entity;
    }
}

