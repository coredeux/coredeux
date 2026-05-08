package com.coredeux.core.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;

class CoredeuxDataAccessServiceTest {

    @Test
    void shouldAllowDefaultRefreshImplementation() {
        CoredeuxDataAccessService service = new CoredeuxDataAccessService() {
            @Override
            public <T> T load(String id, Class<T> type) {
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
            public <T> void remove(T entity) {
            }

            @Override
            public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize,
                    int currentPage) {
                return SearchResult.<T>builder().results(List.of()).build();
            }

            @Override
            public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                    int currentPage) {
                return SearchResult.<T>builder().results(List.of()).build();
            }
        };

        assertDoesNotThrow(() -> service.refresh(new Object()));
    }

    @Test
    void shouldExposeDefaultSupportedComparators() {
        CoredeuxDataAccessService service = new NoOpDataAccessService();

        assertEquals(java.util.Set.of("EQUALS"), service.supportedComparators(Object.class));
    }

    private static final class NoOpDataAccessService implements CoredeuxDataAccessService {

        @Override
        public <T> T load(String id, Class<T> type) {
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
        public <T> void remove(T entity) {
        }

        @Override
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
            return SearchResult.<T>builder().results(List.of()).build();
        }

        @Override
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            return SearchResult.<T>builder().results(List.of()).build();
        }
    }
}
