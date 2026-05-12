package com.coredeux.core.elasticsearch.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import com.coredeux.core.elasticsearch.testentity.SampleElasticsearchEntity;
import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

class DefaultCoredeuxElasticsearchDataAccessServiceTest {

    private ElasticsearchGateway gateway;
    private DefaultCoredeuxElasticsearchDataAccessService dataAccessService;

    @BeforeEach
    void setUp() {
        gateway = mock(ElasticsearchGateway.class);
        dataAccessService = new DefaultCoredeuxElasticsearchDataAccessService(gateway, "");
    }

    @Test
    void shouldSaveLoadUpdateRemoveAndRefreshEntity() {
        SampleElasticsearchEntity saved = sample(null, "Alpha", 10, "one");
        when(gateway.index(anyString(), any(), any())).thenAnswer(invocation -> {
            SampleElasticsearchEntity entity = invocation.getArgument(2);
            entity.setId("entity-1");
            return "entity-1";
        });
        when(gateway.get(anyString(), anyString(), eq(SampleElasticsearchEntity.class)))
                .thenReturn(sample("entity-1", "Alpha", 10, "one"));

        String id = dataAccessService.save(saved);
        assertEquals("entity-1", id);
        assertEquals("entity-1", saved.getId());

        SampleElasticsearchEntity loaded = dataAccessService.load("entity-1", SampleElasticsearchEntity.class);
        assertEquals("Alpha", loaded.getName());

        dataAccessService.update(saved);
        dataAccessService.remove(saved);

        when(gateway.get(anyString(), anyString(), eq(SampleElasticsearchEntity.class)))
                .thenReturn(sample("entity-1", "Mutated", 15, "two"));
        dataAccessService.refresh(saved);

        assertEquals("Mutated", saved.getName());
        assertEquals(15, saved.getAge());
    }

    @Test
    void shouldLoadAllUsingSupportedComparatorsAndPagination() {
        when(gateway.count(anyString(), any(Query.class))).thenReturn(3L, 1L);
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(gateway.search(anyString(), queryCaptor.capture(), eq(SampleElasticsearchEntity.class), eq(2), eq(2)))
                .thenReturn(List.of(sample("entity-1", "Alpha", 10, "one")));

        SearchResult<SampleElasticsearchEntity> result = dataAccessService.loadAll(
                List.of(SearchParams.builder().field("name").comparator("EQUALS").value("Alpha").build()),
                SampleElasticsearchEntity.class, 2, 2);

        assertEquals(1, result.getResults().size());
        assertEquals(3L, result.getPagination().getTotalResults());
        assertEquals(1L, result.getPagination().getResultSize());
        assertEquals(2L, result.getPagination().getCurrentPage());
        assertNotNull(queryCaptor.getValue());
    }

    @Test
    void shouldQueryWithNamedParametersAndPagination() {
        when(gateway.count(anyString(), any(Query.class))).thenReturn(3L);
        when(gateway.search(anyString(), any(Query.class), eq(SampleElasticsearchEntity.class), eq(1), eq(1)))
                .thenReturn(List.of(sample("entity-1", "Alpha", 10, "one")));

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
        when(gateway.get(anyString(), anyString(), eq(SampleElasticsearchEntity.class)))
                .thenThrow(new IllegalStateException("boom"));

        assertThrows(CoredeuxDataAccessException.class,
                () -> dataAccessService.load("entity-1", SampleElasticsearchEntity.class));
    }

    @Test
    void shouldApplyPagingToSearchQuery() {
        when(gateway.count(anyString(), any(Query.class))).thenReturn(1L, 1L);
        when(gateway.search(anyString(), any(Query.class), eq(SampleElasticsearchEntity.class), eq(5), eq(2)))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> dataAccessService.loadAll(List.of(), SampleElasticsearchEntity.class, 5, 2));
        verify(gateway).search(anyString(), any(Query.class), eq(SampleElasticsearchEntity.class), eq(5), eq(2));
    }

    @Test
    void shouldApplyNoPagingForInvalidWindow() {
        when(gateway.count(anyString(), any(Query.class))).thenReturn(1L, 1L);
        when(gateway.search(anyString(), any(Query.class), eq(SampleElasticsearchEntity.class), eq(-1), eq(-1)))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> dataAccessService.loadAll(List.of(), SampleElasticsearchEntity.class, -1, -1));
        verify(gateway).search(anyString(), any(Query.class), eq(SampleElasticsearchEntity.class), eq(-1), eq(-1));
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
