package com.coredeux.core.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

class SearchModelCoverageTest {

    @Test
    void shouldExercisePaginationDataConstructorsBuilderAndMutators() {
        PaginationData empty = new PaginationData();
        assertNull(empty.getCurrentPage());
        assertNull(empty.getTotalResults());
        assertNull(empty.getPageSize());
        assertNull(empty.getTotalPages());
        assertNull(empty.getResultSize());

        PaginationData pagination = new PaginationData(1L, 25L, 10L, 3L, 10L);
        assertEquals(1L, pagination.getCurrentPage());
        assertEquals(25L, pagination.getTotalResults());
        assertEquals(10L, pagination.getPageSize());
        assertEquals(3L, pagination.getTotalPages());
        assertEquals(10L, pagination.getResultSize());

        pagination.setCurrentPage(2L);
        pagination.setTotalResults(30L);
        pagination.setPageSize(15L);
        pagination.setTotalPages(2L);
        pagination.setResultSize(15L);

        PaginationData built = PaginationData.builder()
                .currentPage(2L)
                .totalResults(30L)
                .pageSize(15L)
                .totalPages(2L)
                .resultSize(15L)
                .build();

        assertEquals(built, pagination);
        assertEquals(built.hashCode(), pagination.hashCode());
        assertNotEquals(pagination, new PaginationData(1L, 25L, 10L, 3L, 10L));
        assertNotNull(pagination.toString());
    }

    @Test
    void shouldExerciseSearchParamsConstructorsBuilderAndMutators() {
        SearchParams empty = new SearchParams();
        assertNull(empty.getField());
        assertNull(empty.getComparator());
        assertNull(empty.getValue());

        SearchParams params = new SearchParams("status", "eq", "ACTIVE");
        assertEquals("status", params.getField());
        assertEquals("eq", params.getComparator());
        assertEquals("ACTIVE", params.getValue());

        params.setField("code");
        params.setComparator("in");
        params.setValue(List.of("A", "B"));

        SearchParams built = SearchParams.builder()
                .field("code")
                .comparator("in")
                .value(List.of("A", "B"))
                .build();

        assertEquals(built, params);
        assertEquals(built.hashCode(), params.hashCode());
        assertNotEquals(params, new SearchParams("status", "eq", "ACTIVE"));
        assertNotNull(params.toString());
    }

    @Test
    void shouldExerciseSearchResultConstructorsBuilderAndMutators() {
        SearchResult<String> empty = new SearchResult<>();
        assertNull(empty.getResults());
        assertNull(empty.getPagination());

        PaginationData pagination = PaginationData.builder()
                .currentPage(1L)
                .totalResults(2L)
                .pageSize(10L)
                .totalPages(1L)
                .resultSize(2L)
                .build();
        List<String> results = List.of("one", "two");

        SearchResult<String> searchResult = new SearchResult<>(results, pagination);
        assertSame(results, searchResult.getResults());
        assertSame(pagination, searchResult.getPagination());

        PaginationData updatedPagination = PaginationData.builder()
                .currentPage(2L)
                .totalResults(4L)
                .pageSize(2L)
                .totalPages(2L)
                .resultSize(2L)
                .build();
        List<String> updatedResults = List.of("three", "four");
        searchResult.setResults(updatedResults);
        searchResult.setPagination(updatedPagination);

        SearchResult<String> built = SearchResult.<String>builder()
                .results(updatedResults)
                .pagination(updatedPagination)
                .build();

        assertEquals(built, searchResult);
        assertEquals(built.hashCode(), searchResult.hashCode());
        assertNotEquals(searchResult, new SearchResult<>(results, pagination));
        assertNotNull(searchResult.toString());
    }
}
