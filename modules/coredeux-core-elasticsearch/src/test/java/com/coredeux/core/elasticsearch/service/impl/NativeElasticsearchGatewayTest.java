package com.coredeux.core.elasticsearch.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;

class NativeElasticsearchGatewayTest {

    @Test
    void shouldMapClientOperations() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        NativeElasticsearchGateway gateway = new NativeElasticsearchGateway(client);

        GetResponse<SampleEntity> getResponse = mock(GetResponse.class);
        when(getResponse.found()).thenReturn(true);
        when(getResponse.source()).thenReturn(new SampleEntity("1"));
        when(client.get(org.mockito.ArgumentMatchers.<Function<GetRequest.Builder, ObjectBuilder<GetRequest>>>any(),
                eq(SampleEntity.class))).thenReturn(getResponse);

        IndexResponse indexResponse = mock(IndexResponse.class);
        when(indexResponse.id()).thenReturn("1");
        when(client.index(org.mockito.ArgumentMatchers.<Function<IndexRequest.Builder<SampleEntity>, ObjectBuilder<IndexRequest<SampleEntity>>>>any()))
                .thenReturn(indexResponse);

        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        when(client.delete(org.mockito.ArgumentMatchers.<Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>>any()))
                .thenReturn(deleteResponse);

        CountResponse countResponse = mock(CountResponse.class);
        when(countResponse.count()).thenReturn(2L);
        when(client.count(org.mockito.ArgumentMatchers.<Function<CountRequest.Builder, ObjectBuilder<CountRequest>>>any()))
                .thenReturn(countResponse);

        SearchResponse<SampleEntity> searchResponse = mock(SearchResponse.class);
        Hit<SampleEntity> hit = mock(Hit.class);
        when(hit.source()).thenReturn(new SampleEntity("2"));
        co.elastic.clients.elasticsearch.core.search.HitsMetadata<SampleEntity> hits = mock(
                co.elastic.clients.elasticsearch.core.search.HitsMetadata.class);
        when(hits.hits()).thenReturn(List.of(hit));
        when(searchResponse.hits()).thenReturn(hits);
        when(client.search(org.mockito.ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
                eq(SampleEntity.class))).thenReturn(searchResponse);

        assertEquals("1", gateway.index("index", "1", new SampleEntity("1")));
        assertEquals(2L, gateway.count("index", mock(Query.class)));
        assertEquals("1", gateway.get("index", "1", SampleEntity.class).id());
        assertEquals(1, gateway.search("index", mock(Query.class), SampleEntity.class, 10, 1).size());
        assertDoesNotThrow(() -> gateway.delete("index", "1"));
    }

    @Test
    void shouldReturnNullOrEmptyWhenClientReturnsNothing() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        NativeElasticsearchGateway gateway = new NativeElasticsearchGateway(client);
        when(client.get(org.mockito.ArgumentMatchers.<Function<GetRequest.Builder, ObjectBuilder<GetRequest>>>any(),
                eq(SampleEntity.class))).thenReturn(null);
        when(client.count(org.mockito.ArgumentMatchers.<Function<CountRequest.Builder, ObjectBuilder<CountRequest>>>any()))
                .thenReturn(null);
        when(client.search(org.mockito.ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
                eq(SampleEntity.class))).thenReturn(null);

        assertNull(gateway.get("index", "1", SampleEntity.class));
        assertEquals(0L, gateway.count("index", mock(Query.class)));
        assertEquals(List.of(), gateway.search("index", mock(Query.class), SampleEntity.class, 0, 0));
    }

    @Test
    void shouldReturnNullWhenDocumentIsNotFound() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        NativeElasticsearchGateway gateway = new NativeElasticsearchGateway(client);
        GetResponse<SampleEntity> getResponse = mock(GetResponse.class);
        when(getResponse.found()).thenReturn(false);
        when(client.get(org.mockito.ArgumentMatchers.<Function<GetRequest.Builder, ObjectBuilder<GetRequest>>>any(),
                eq(SampleEntity.class))).thenReturn(getResponse);

        assertNull(gateway.get("index", "1", SampleEntity.class));
    }

    @Test
    void shouldExposeFactoryAndWrapIOExceptionFailures() throws Exception {
        RestClient restClient = mock(RestClient.class);
        assertNotNull(NativeElasticsearchGateway.from(restClient));

        ElasticsearchClient client = mock(ElasticsearchClient.class);
        NativeElasticsearchGateway gateway = new NativeElasticsearchGateway(client);
        when(client.get(org.mockito.ArgumentMatchers.<Function<GetRequest.Builder, ObjectBuilder<GetRequest>>>any(),
                eq(SampleEntity.class))).thenThrow(new IOException("boom"));
        when(client.index(org.mockito.ArgumentMatchers.<Function<IndexRequest.Builder<SampleEntity>, ObjectBuilder<IndexRequest<SampleEntity>>>>any()))
                .thenThrow(new IOException("boom"));
        when(client.delete(org.mockito.ArgumentMatchers.<Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>>any()))
                .thenThrow(new IOException("boom"));
        when(client.count(org.mockito.ArgumentMatchers.<Function<CountRequest.Builder, ObjectBuilder<CountRequest>>>any()))
                .thenThrow(new IOException("boom"));
        when(client.search(org.mockito.ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
                eq(SampleEntity.class))).thenThrow(new IOException("boom"));

        assertThrows(IllegalStateException.class, () -> gateway.get("index", "1", SampleEntity.class));
        assertThrows(IllegalStateException.class, () -> gateway.index("index", "1", new SampleEntity("1")));
        assertThrows(IllegalStateException.class, () -> gateway.delete("index", "1"));
        assertThrows(IllegalStateException.class, () -> gateway.count("index", mock(Query.class)));
        assertThrows(IllegalStateException.class,
                () -> gateway.search("index", mock(Query.class), SampleEntity.class, 10, 1));
    }

    @Test
    void shouldPropagateRuntimeExceptionsWithoutWrapping() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        NativeElasticsearchGateway gateway = new NativeElasticsearchGateway(client);

        when(client.get(org.mockito.ArgumentMatchers.<Function<GetRequest.Builder, ObjectBuilder<GetRequest>>>any(),
                eq(SampleEntity.class))).thenThrow(new IllegalStateException("boom"));
        when(client.index(org.mockito.ArgumentMatchers.<Function<IndexRequest.Builder<SampleEntity>, ObjectBuilder<IndexRequest<SampleEntity>>>>any()))
                .thenThrow(new IllegalStateException("boom"));
        when(client.delete(org.mockito.ArgumentMatchers.<Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>>any()))
                .thenThrow(new IllegalStateException("boom"));
        when(client.count(org.mockito.ArgumentMatchers.<Function<CountRequest.Builder, ObjectBuilder<CountRequest>>>any()))
                .thenThrow(new IllegalStateException("boom"));
        when(client.search(org.mockito.ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
                eq(SampleEntity.class))).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> gateway.get("index", "1", SampleEntity.class));
        assertThrows(IllegalStateException.class, () -> gateway.index("index", "", new SampleEntity("1")));
        assertThrows(IllegalStateException.class, () -> gateway.delete("index", "1"));
        assertThrows(IllegalStateException.class, () -> gateway.count("index", mock(Query.class)));
        assertThrows(IllegalStateException.class,
                () -> gateway.search("index", mock(Query.class), SampleEntity.class, 10, 1));
    }

    private record SampleEntity(String id) {
    }
}
