package com.coredeux.core.elasticsearch.service.impl;

import java.util.List;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import org.elasticsearch.client.RestClient;

final class NativeElasticsearchGateway implements ElasticsearchGateway {

    private final ElasticsearchClient client;

    NativeElasticsearchGateway(ElasticsearchClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    static NativeElasticsearchGateway from(RestClient restClient) {
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new NativeElasticsearchGateway(new ElasticsearchClient(transport));
    }

    @Override
    public <T> T get(String indexName, String id, Class<T> type) {
        try {
            GetResponse<T> response = client.get(request -> request.index(indexName).id(id), type);
            return response == null || !response.found() ? null : response.source();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load document from Elasticsearch", exception);
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    @Override
    public <T> String index(String indexName, String id, T entity) {
        try {
            IndexResponse response = client.index(request -> {
                request.index(indexName);
                if (id != null && !id.isBlank()) {
                    request.id(id);
                }
                request.document(entity);
                return request;
            });
            return response.id();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save document to Elasticsearch", exception);
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    @Override
    public void delete(String indexName, String id) {
        try {
            client.delete(request -> request.index(indexName).id(id));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete document from Elasticsearch", exception);
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    @Override
    public long count(String indexName, Query query) {
        try {
            CountResponse response = client.count(request -> request.index(indexName).query(query));
            return response == null ? 0L : response.count();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to count Elasticsearch documents", exception);
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    @Override
    public <T> List<T> search(String indexName, Query query, Class<T> type, int pageSize, int currentPage) {
        try {
            SearchResponse<T> response = client.search(request -> {
                request.index(indexName);
                request.query(query);
                if (pageSize > 0 && currentPage > 0) {
                    request.from(Math.max(currentPage - 1, 0) * pageSize);
                    request.size(pageSize);
                }
                return request;
            }, type);
            if (response == null || response.hits() == null) {
                return List.of();
            }
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to search Elasticsearch documents", exception);
        } catch (RuntimeException exception) {
            throw exception;
        }
    }
}
