package com.coredeux.core.elasticsearch.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;

import com.coredeux.core.elasticsearch.testentity.SampleElasticsearchEntity;
import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

class DefaultCoredeuxElasticsearchDataAccessServiceTest {

    private ElasticsearchOperations elasticsearchOperations;
    private DefaultCoredeuxElasticsearchDataAccessService dataAccessService;

    @BeforeEach
    void setUp() {
        elasticsearchOperations = mock(ElasticsearchOperations.class);
        dataAccessService = new DefaultCoredeuxElasticsearchDataAccessService(elasticsearchOperations, "");
    }

    @Test
    void shouldSaveLoadUpdateRemoveAndRefreshEntity() {
        SampleElasticsearchEntity saved = sample(null, "Alpha", 10, "one");
        when(elasticsearchOperations.save(any(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenAnswer(invocation -> {
                    SampleElasticsearchEntity entity = invocation.getArgument(0);
                    entity.setId("entity-1");
                    return entity;
                });
        when(elasticsearchOperations.get(anyString(), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(sample("entity-1", "Alpha", 10, "one"));

        String id = dataAccessService.save(saved);
        assertEquals("entity-1", id);
        assertEquals("entity-1", saved.getId());

        SampleElasticsearchEntity loaded = dataAccessService.load("entity-1", SampleElasticsearchEntity.class);
        assertEquals("Alpha", loaded.getName());

        dataAccessService.update(saved);
        dataAccessService.remove(saved);

        when(elasticsearchOperations.get(anyString(), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(sample("entity-1", "Mutated", 15, "two"));
        dataAccessService.refresh(saved);

        assertEquals("Mutated", saved.getName());
        assertEquals(15, saved.getAge());
    }

    @Test
    void shouldLoadAllUsingSupportedComparatorsAndPagination() {
        SearchHits<SampleElasticsearchEntity> hits = mock(SearchHits.class);
        SearchHit<SampleElasticsearchEntity> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(sample("entity-1", "Alpha", 10, "one"));
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(hits.getTotalHits()).thenReturn(3L);

        when(elasticsearchOperations.count(any(Query.class), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(3L, 1L);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(hits);

        SearchResult<SampleElasticsearchEntity> result = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("EQUALS").value("Alpha").build()),
                SampleElasticsearchEntity.class, 2, 2);

        assertEquals(1, result.getResults().size());
        assertEquals(3L, result.getPagination().getTotalResults());
        assertEquals(1L, result.getPagination().getResultSize());
        assertEquals(2L, result.getPagination().getCurrentPage());
        assertNotNull(queryCaptor.getValue().getPageable());
        assertEquals(1, queryCaptor.getValue().getPageable().getPageNumber());
        assertEquals(2, queryCaptor.getValue().getPageable().getPageSize());
    }

    @Test
    void shouldQueryWithNamedParametersAndPagination() {
        SearchHits<SampleElasticsearchEntity> hits = mock(SearchHits.class);
        SearchHit<SampleElasticsearchEntity> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(sample("entity-1", "Alpha", 10, "one"));
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(hits.getTotalHits()).thenReturn(3L);

        when(elasticsearchOperations.count(any(StringQuery.class), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(3L);
        when(elasticsearchOperations.search(any(Query.class), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(hits);

        SearchResult<SampleElasticsearchEntity> result = dataAccessService.query(
                """
                        {"query":{"term":{"age":{{age}}}}}
                        """,
                Map.of("age", 10), SampleElasticsearchEntity.class, 1, 1);

        assertEquals(1, result.getResults().size());
        assertEquals(3L, result.getPagination().getTotalResults());
        assertEquals(3L, result.getPagination().getResultSize());
    }

    @Test
    void shouldExposeElasticsearchComparators() {
        assertFalse(dataAccessService.supportedComparators(SampleElasticsearchEntity.class).isEmpty());
        assertTrue(dataAccessService.supportedComparators(SampleElasticsearchEntity.class).contains("ANYWHERE"));
    }

    @Test
    void shouldRejectInvalidInputs() {
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.load(null, SampleElasticsearchEntity.class));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.load("entity-1", null));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.loadAll(List.of(), null, 10, 1));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.query(" ", Map.of(), SampleElasticsearchEntity.class, 10, 1));
        assertThrows(CoredeuxValidationException.class,
                () -> dataAccessService.loadAll(
                        List.of(SearchParams.builder().field("name").comparator("UNKNOWN").value("x").build()),
                        SampleElasticsearchEntity.class, 10, 1));
    }

    @Test
    void shouldWrapRuntimeFailures() {
        when(elasticsearchOperations.get(anyString(), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenThrow(new IllegalStateException("boom"));

        assertThrows(CoredeuxDataAccessException.class,
                () -> dataAccessService.load("entity-1", SampleElasticsearchEntity.class));
    }

    @Test
    void shouldApplyPagingToSearchQuery() {
        when(elasticsearchOperations.count(any(Query.class), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(1L, 1L);
        SearchHits<SampleElasticsearchEntity> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(hits);

        assertDoesNotThrow(() -> dataAccessService.loadAll(List.of(), SampleElasticsearchEntity.class, 5, 2));
        assertNotNull(queryCaptor.getValue().getPageable());
        assertEquals(1, queryCaptor.getValue().getPageable().getPageNumber());
        assertEquals(5, queryCaptor.getValue().getPageable().getPageSize());
    }

    @Test
    void shouldApplyNoPagingForInvalidWindow() {
        when(elasticsearchOperations.count(any(Query.class), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(1L, 1L);
        SearchHits<SampleElasticsearchEntity> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(elasticsearchOperations.search(queryCaptor.capture(), eq(SampleElasticsearchEntity.class), any(IndexCoordinates.class)))
                .thenReturn(hits);

        assertDoesNotThrow(() -> dataAccessService.loadAll(List.of(), SampleElasticsearchEntity.class, -1, -1));
        assertNotNull(queryCaptor.getValue().getPageable());
        assertEquals(0, queryCaptor.getValue().getPageable().getPageNumber());
        assertEquals(10, queryCaptor.getValue().getPageable().getPageSize());
    }

    private SampleElasticsearchEntity sample(String id, String name, Integer age, String payload) {
        SampleElasticsearchEntity entity = new SampleElasticsearchEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setAge(age);
        entity.setPayload(payload);
        return entity;
    }
}
