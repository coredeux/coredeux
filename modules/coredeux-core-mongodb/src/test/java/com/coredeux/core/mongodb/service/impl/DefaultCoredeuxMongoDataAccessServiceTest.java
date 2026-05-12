package com.coredeux.core.mongodb.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.mongodb.testentity.SampleMongoEntity;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

class DefaultCoredeuxMongoDataAccessServiceTest {

    private MongoDatabase mongoDatabase;
    private MongoCollection<Document> collection;
    private DefaultCoredeuxMongoDataAccessService dataAccessService;

    @BeforeEach
    void setUp() {
        mongoDatabase = mock(MongoDatabase.class);
        collection = mock(MongoCollection.class);
        when(mongoDatabase.getCollection(anyString())).thenReturn(collection);
        dataAccessService = new DefaultCoredeuxMongoDataAccessService(mongoDatabase);
    }

    @Test
    void shouldSaveLoadUpdateRemoveAndRefreshEntity() {
        SampleMongoEntity saved = sample(null, "Alpha", 10, List.of("one", "two"));
        AtomicReference<String> savedId = new AtomicReference<>(new ObjectId().toHexString());
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(1L);
        DeleteResult deleteResult = mock(DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(1L);

        FindIterable<Document> findIterable = mock(FindIterable.class);
        when(findIterable.first()).thenAnswer(invocation -> new Document("_id", savedId.get())
                .append("name", "Alpha")
                .append("age", 10)
                .append("tags", List.of("one", "two")));
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(collection.replaceOne(any(Bson.class), any(Document.class), any())).thenAnswer(invocation -> {
            savedId.set(invocation.getArgument(1, Document.class).getString("_id"));
            return updateResult;
        });
        when(collection.deleteOne(any(Bson.class))).thenReturn(deleteResult);

        String id = dataAccessService.save(saved);
        assertNotNull(id);
        assertEquals(saved.getId(), id);

        String generatedId = id;
        SampleMongoEntity loaded = dataAccessService.load(savedId.get(), SampleMongoEntity.class);
        assertEquals("Alpha", loaded.getName());

        SampleMongoEntity updateCandidate = sample(generatedId, "Updated", 15, List.of("two"));
        when(collection.replaceOne(any(Bson.class), any(Document.class), any())).thenReturn(updateResult);
        dataAccessService.update(updateCandidate);
        verify(collection, times(2)).replaceOne(any(Bson.class), any(Document.class), any());

        dataAccessService.remove(updateCandidate);
        verify(collection).deleteOne(any(Bson.class));

        Document refreshedDocument = new Document("_id", generatedId)
                .append("name", "Refreshed")
                .append("age", 99)
                .append("tags", List.of("refresh"));
        when(findIterable.first()).thenReturn(refreshedDocument);
        dataAccessService.refresh(updateCandidate);
        assertEquals("Refreshed", updateCandidate.getName());
        assertEquals(99, updateCandidate.getAge());
    }

    @Test
    void shouldLoadAllUsingSupportedComparatorsAndPagination() {
        FindIterable<Document> documents = mock(FindIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);
        when(collection.countDocuments()).thenReturn(3L);
        when(collection.countDocuments(any(Bson.class))).thenReturn(1L);
        when(collection.find(any(Bson.class))).thenReturn(documents);
        when(documents.skip(0)).thenReturn(documents);
        when(documents.limit(10)).thenReturn(documents);
        when(documents.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(
                new Document("_id", "1").append("name", "Alpha").append("age", 10).append("tags", List.of("common")));

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
        FindIterable<Document> documents = mock(FindIterable.class);
        MongoCursor<Document> cursor = mock(MongoCursor.class);
        when(collection.countDocuments(any(Bson.class))).thenReturn(2L);
        when(collection.find(any(Bson.class))).thenReturn(documents);
        when(documents.skip(0)).thenReturn(documents);
        when(documents.limit(1)).thenReturn(documents);
        when(documents.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(new Document("_id", "1").append("name", "Alpha").append("age", 10)
                .append("tags", List.of()));

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
        assertThrows(CoredeuxValidationException.class, () -> dataAccessService.buildCriteria(
                SearchParams.builder().field("name").comparator("UNKNOWN").value("x").build()));
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
        Bson filter = assertDoesNotThrow(() -> dataAccessService.buildSearchFilter(List.of(
                SearchParams.builder().field("name").comparator("EQUALS").value(null).build())));
        assertNotNull(filter);
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
        @org.springframework.data.annotation.Id
        private ObjectId id;
    }
}
