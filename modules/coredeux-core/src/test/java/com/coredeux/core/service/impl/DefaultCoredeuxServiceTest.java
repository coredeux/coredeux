package com.coredeux.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.core.strategy.CoredeuxStrategy;

class DefaultCoredeuxServiceTest {

    @Test
    void shouldDelegateReadOperationsToStrategy() {
        RecordingStrategy strategy = new RecordingStrategy();
        CoredeuxService service = new DefaultCoredeuxService(strategy);
        SearchResult<SampleEntity> searchResult = SearchResult.<SampleEntity>builder().results(List.of()).build();
        strategy.loadResult = new SampleEntity("1");
        strategy.queryResult = searchResult;
        strategy.loadAllResult = searchResult;

        SampleEntity loaded = service.load("1", SampleEntity.class);
        assertEquals("load", strategy.lastInvocation);
        assertSame(strategy.loadResult, loaded);

        SearchResult<SampleEntity> queried = service.query("from SampleEntity", Map.of(), SampleEntity.class, 10, 1);
        assertEquals("query", strategy.lastInvocation);
        assertSame(searchResult, queried);

        SearchResult<SampleEntity> all = service.loadAll(List.of(), SampleEntity.class, 10, 1);
        assertEquals("loadAll", strategy.lastInvocation);
        assertSame(searchResult, all);

        Set<String> comparators = service.supportedComparators(SampleEntity.class);
        assertEquals("supportedComparators", strategy.lastInvocation);
        assertEquals(Set.of("EQUALS", "STARTSWITH"), comparators);
    }

    @Test
    void shouldDelegateWriteOperationsToStrategy() {
        RecordingStrategy strategy = new RecordingStrategy();
        CoredeuxService service = new DefaultCoredeuxService(strategy);
        SampleEntity entity = new SampleEntity("1");
        strategy.saveResult = "saved-id";

        String saveId = service.save(entity);
        assertEquals("save", strategy.lastInvocation);
        assertEquals("saved-id", saveId);

        service.update(entity);
        assertEquals("update", strategy.lastInvocation);
        assertTrue(strategy.updateInvoked);

        service.remove("1", SampleEntity.class);
        assertEquals("removeById", strategy.lastInvocation);
        assertTrue(strategy.removeByIdInvoked);

        service.remove(entity);
        assertEquals("removeEntity", strategy.lastInvocation);
        assertTrue(strategy.removeEntityInvoked);

        service.refresh(entity);
        assertEquals("refresh", strategy.lastInvocation);
        assertTrue(strategy.refreshInvoked);
    }

    private static final class SampleEntity {

        private final String id;

        private SampleEntity(String id) {
            this.id = id;
        }

        @SuppressWarnings("unused")
        public String getId() {
            return id;
        }
    }

    private static final class RecordingStrategy implements CoredeuxStrategy {

        private String lastInvocation;
        private Object loadResult;
        private SearchResult<?> queryResult;
        private SearchResult<?> loadAllResult;
        private Set<String> supportedComparatorsResult = Set.of("EQUALS", "STARTSWITH");
        private String saveResult;
        private boolean updateInvoked;
        private boolean removeByIdInvoked;
        private boolean removeEntityInvoked;
        private boolean refreshInvoked;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T load(String id, Class<T> type) {
            lastInvocation = "load";
            return (T) loadResult;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            lastInvocation = "query";
            return (SearchResult<T>) queryResult;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
            lastInvocation = "loadAll";
            return (SearchResult<T>) loadAllResult;
        }

        @Override
        public Set<String> supportedComparators(Class<?> type) {
            lastInvocation = "supportedComparators";
            return supportedComparatorsResult;
        }

        @Override
        public <T> String save(T entity) {
            lastInvocation = "save";
            return saveResult;
        }

        @Override
        public <T> void update(T entity) {
            lastInvocation = "update";
            updateInvoked = true;
        }

        @Override
        public <T> void remove(String id, Class<T> type) {
            lastInvocation = "removeById";
            removeByIdInvoked = true;
        }

        @Override
        public <T> void remove(T entity) {
            lastInvocation = "removeEntity";
            removeEntityInvoked = true;
        }

        @Override
        public <T> void refresh(T entity) {
            lastInvocation = "refresh";
            refreshInvoked = true;
        }
    }
}
