package com.coredeux.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.strategy.CoredeuxStrategy;

class CoredeuxServiceContractTest {

    @Test
    void publicServiceAndStrategyExposeDefaultComparator() {
        assertEquals(Set.of("EQUALS"), new StubCoredeuxService().supportedComparators(String.class));
        assertEquals(Set.of("EQUALS"), new StubCoredeuxStrategy().supportedComparators(String.class));
    }

    static class StubCoredeuxService implements CoredeuxService {

        @Override
        public <T> T load(String id, Class<T> type) {
            return null;
        }

        @Override
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            return null;
        }

        @Override
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
            return null;
        }

        @Override
        public <T> String save(T entity) {
            return null;
        }

        @Override
        public <T> void update(T entity) {
        }

        @Override
        public <T> void remove(String id, Class<T> type) {
        }

        @Override
        public <T> void remove(T entity) {
        }

        @Override
        public <T> void refresh(T entity) {
        }
    }

    static class StubCoredeuxStrategy implements CoredeuxStrategy {

        @Override
        public <T> T load(String id, Class<T> type) {
            return null;
        }

        @Override
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            return null;
        }

        @Override
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
            return null;
        }

        @Override
        public <T> String save(T entity) {
            return null;
        }

        @Override
        public <T> void update(T entity) {
        }

        @Override
        public <T> void remove(String id, Class<T> type) {
        }

        @Override
        public <T> void remove(T entity) {
        }

        @Override
        public <T> void refresh(T entity) {
        }
    }
}
