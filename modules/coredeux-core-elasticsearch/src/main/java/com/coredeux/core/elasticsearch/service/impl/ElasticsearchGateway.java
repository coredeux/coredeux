package com.coredeux.core.elasticsearch.service.impl;

import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

interface ElasticsearchGateway {

    <T> T get(String indexName, String id, Class<T> type);

    <T> String index(String indexName, String id, T entity);

    void delete(String indexName, String id);

    long count(String indexName, Query query);

    <T> List<T> search(String indexName, Query query, Class<T> type, int pageSize, int currentPage);
}
