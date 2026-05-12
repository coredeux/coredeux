package com.coredeux.core.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.core.strategy.CoredeuxStrategy;

/**
 * Default public service implementation that delegates entity operations to the
 * configured strategy.
 */
public class DefaultCoredeuxService implements CoredeuxService {

    private final CoredeuxStrategy coredeuxStrategy;

    public DefaultCoredeuxService(CoredeuxStrategy coredeuxStrategy) {
        this.coredeuxStrategy = coredeuxStrategy;
    }

    @Override
    public <T> T load(String id, Class<T> type) {
        return coredeuxStrategy.load(id, type);
    }

    @Override
    public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
            int currentPage) {
        return coredeuxStrategy.query(query, params, type, pageSize, currentPage);
    }

    @Override
    public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
        return coredeuxStrategy.loadAll(params, type, pageSize, currentPage);
    }

    @Override
    public Set<String> supportedComparators(Class<?> type) {
        return coredeuxStrategy.supportedComparators(type);
    }

    @Override
    public <T> String save(T entity) {
        return coredeuxStrategy.save(entity);
    }

    @Override
    public <T> void update(T entity) {
        coredeuxStrategy.update(entity);
    }

    @Override
    public <T> void remove(String id, Class<T> type) {
        coredeuxStrategy.remove(id, type);
    }

    @Override
    public <T> void remove(T entity) {
        coredeuxStrategy.remove(entity);
    }

    @Override
    public <T> void refresh(T entity) {
        coredeuxStrategy.refresh(entity);
    }
}
