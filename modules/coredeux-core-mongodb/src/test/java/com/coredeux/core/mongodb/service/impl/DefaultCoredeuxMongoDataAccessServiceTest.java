package com.coredeux.core.mongodb.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.mongodb.testentity.SampleMongoEntity;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

class DefaultCoredeuxMongoDataAccessServiceTest {

    private MongoTemplate mongoTemplate;
    private DefaultCoredeuxMongoDataAccessService dataAccessService;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        dataAccessService = new DefaultCoredeuxMongoDataAccessService(mongoTemplate);
    }

    @Test
    void shouldSaveLoadUpdateRemoveAndRefreshEntity() {
        SampleMongoEntity saved = sample(null, "Alpha", 10, List.of("one", "two"));
        String generatedId = new ObjectId().toHexString();

        when(mongoTemplate.save(any(SampleMongoEntity.class))).thenAnswer(invocation -> {
            SampleMongoEntity entity = invocation.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });
        when(mongoTemplate.findById(eq(generatedId), eq(SampleMongoEntity.class))).thenReturn(sample(generatedId,
                "Alpha", 10, List.of("one", "two")));

        String id = dataAccessService.save(saved);
        assertEquals(generatedId, id);
        assertEquals(generatedId, saved.getId());

        SampleMongoEntity loaded = dataAccessService.load(generatedId, SampleMongoEntity.class);
        assertEquals("Alpha", loaded.getName());

        SampleMongoEntity updateCandidate = sample(generatedId, "Updated", 15, List.of("two"));
        dataAccessService.update(updateCandidate);
        verify(mongoTemplate).save(updateCandidate);

        dataAccessService.remove(updateCandidate);
        verify(mongoTemplate).remove(updateCandidate);

        when(mongoTemplate.findById(eq(generatedId), eq(SampleMongoEntity.class))).thenReturn(sample(generatedId,
                "Refreshed", 99, List.of("refresh")));
        dataAccessService.refresh(updateCandidate);
        assertEquals("Refreshed", updateCandidate.getName());
        assertEquals(99, updateCandidate.getAge());
    }

    @Test
    void shouldLoadAllUsingSupportedComparatorsAndPagination() {
        when(mongoTemplate.count(any(Query.class), eq(SampleMongoEntity.class))).thenAnswer(invocation -> {
            Query query = invocation.getArgument(0);
            return query.getQueryObject().isEmpty() ? 3L : 1L;
        });
        when(mongoTemplate.find(any(Query.class), eq(SampleMongoEntity.class)))
                .thenReturn(List.of(sample("1", "Alpha", 10, List.of("common"))));

        SearchResult<SampleMongoEntity> result = dataAccessService.loadAll(List.of(
                SearchParams.builder().field("name").comparator("EQUALS").value("Alpha").build(),
                SearchParams.builder().field("tags").comparator("CONTAINS").value("common").build()),
                SampleMongoEntity.class, 10, 1);

        assertEquals(1, result.getResults().size());
        assertEquals(3L, result.getPagination().getTotalResults());
        assertEquals(1L, result.getPagination().getResultSize());
        assertEquals(1L, result.getPagination().getTotalPages());
    }

    @Test
    void shouldQueryWithTemplateParametersAndPagination() {
        when(mongoTemplate.count(any(Query.class), eq(SampleMongoEntity.class))).thenReturn(2L);
        when(mongoTemplate.find(any(Query.class), eq(SampleMongoEntity.class)))
                .thenReturn(List.of(sample("1", "Alpha", 10, List.of())));

        SearchResult<SampleMongoEntity> result = dataAccessService.query(
                "{\"name\": {{name}}, \"age\": {\"$gte\": {{minAge}}}}",
                Map.of("name", "Alpha", "minAge", 10),
                SampleMongoEntity.class, 1, 1);

        assertEquals(1, result.getResults().size());
        assertEquals(2L, result.getPagination().getTotalResults());
        assertEquals(2L, result.getPagination().getResultSize());
        assertEquals(2L, result.getPagination().getTotalPages());
    }

    @Test
    void shouldAdvertiseMongoComparators() {
        assertTrue(dataAccessService.supportedComparators(SampleMongoEntity.class).contains("ANYWHERE"));
        assertTrue(dataAccessService.supportedComparators(SampleMongoEntity.class).contains("CONTAINS"));
    }

    @Test
    void shouldFailForInvalidInputsAndUnsupportedComparators() {
        assertThrows(CoredeuxValidationException.class, () -> dataAccessService.load(null, SampleMongoEntity.class));
        assertThrows(CoredeuxValidationException.class, () -> dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("UNKNOWN").value("x").build()),
                SampleMongoEntity.class, 10, 1));
        assertThrows(CoredeuxValidationException.class, () -> dataAccessService.query(" ", Map.of(),
                SampleMongoEntity.class, 10, 1));
    }

    @Test
    void shouldWrapIdentifierConversionErrors() {
        assertThrows(CoredeuxDataAccessException.class,
                () -> dataAccessService.load("not-a-valid-object-id", SampleObjectIdEntity.class));
    }

    @Test
    void shouldAllowNullComparatorValuesToBeIgnored() {
        when(mongoTemplate.count(any(Query.class), eq(SampleMongoEntity.class))).thenReturn(3L);
        when(mongoTemplate.find(any(Query.class), eq(SampleMongoEntity.class))).thenReturn(List.of(
                sample("1", "Alpha", 10, List.of()),
                sample("2", "Beta", 20, List.of()),
                sample("3", "Gamma", 30, List.of())));

        SearchResult<SampleMongoEntity> result = assertDoesNotThrow(() -> dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("EQUALS").value(null).build()),
                SampleMongoEntity.class, 10, 1));

        assertEquals(3, result.getResults().size());
        assertEquals(3L, result.getPagination().getResultSize());
    }

    private SampleMongoEntity sample(String id, String name, Integer age, List<String> tags) {
        SampleMongoEntity entity = new SampleMongoEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setAge(age);
        entity.setTags(tags);
        return entity;
    }

    private static class SampleObjectIdEntity {
        @Id
        private ObjectId id;
    }
}
