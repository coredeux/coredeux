package com.coredeux.demo.drl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.demo.domain.DrlRuleRecord;
import com.coredeux.drl.converter.AnnotationBasedJavaToDrlConverter;
import com.coredeux.drl.converter.ConvertedDrl;

@ExtendWith(MockitoExtension.class)
class DemoDrlRuleSourceServiceTest {

    @Mock
    private CoredeuxService coredeuxService;

    private AnnotationBasedJavaToDrlConverter converter;

    private DemoDrlRuleSourceService service;

    @BeforeEach
    void setUp() {
        service = new DemoDrlRuleSourceService(coredeuxService);
        converter = org.mockito.Mockito.spy(new AnnotationBasedJavaToDrlConverter());
        ReflectionTestUtils.setField(service, "converter", converter);
    }

    @Test
    void resolvesFindsListsSavesDeletesAndConvertsRules() {
        DrlRuleRecord record = new DrlRuleRecord();
        record.setPk(1L);
        record.setCode("customerValidator.drl");
        record.setDrl("rule \"validate\"");
        SearchResult<DrlRuleRecord> empty = SearchResult.<DrlRuleRecord>builder().results(List.of()).build();
        SearchResult<DrlRuleRecord> found = SearchResult.<DrlRuleRecord>builder().results(List.of(record)).build();

        lenient().when(coredeuxService.loadAll(eq(Collections.emptyList()), eq(DrlRuleRecord.class), eq(-1), eq(-1)))
                .thenReturn(SearchResult.<DrlRuleRecord>builder().results(List.of(record)).build());
        lenient().when(coredeuxService.loadAll(argThat(list -> list != null && !list.isEmpty()), eq(DrlRuleRecord.class), eq(-1), eq(-1)))
                .thenReturn(found);
        lenient().when(coredeuxService.save(any())).thenReturn("1");

        assertEquals(List.of(record), service.findAll());
        assertEquals(Optional.empty(), service.findByCode(" "));
        assertEquals("rule \"validate\"", service.resolve("customerValidator.drl"));

        DrlRuleRecord saved = service.save("customerValidator.drl", "desc", "rule \"validate\"");
        assertEquals("customerValidator.drl", saved.getCode());
        verify(coredeuxService).update(saved);
        verify(coredeuxService).refresh(saved);

        doReturn(new ConvertedDrl("customerValidator.drl", "generated drl")).when(converter).convert("source");
        DrlRuleRecord converted = service.convertAndSave("source");
        assertEquals("customerValidator.drl", converted.getCode());
        assertEquals("generated drl", converted.getDrl());

        service.delete("customerValidator.drl");
        verify(coredeuxService).remove(record);
    }

    @Test
    void rejectsBlankInputsAndMissingRules() {
        SearchResult<DrlRuleRecord> empty = SearchResult.<DrlRuleRecord>builder().results(List.of()).build();
        lenient().when(coredeuxService.loadAll(eq(Collections.emptyList()), eq(DrlRuleRecord.class), eq(-1), eq(-1))).thenReturn(empty);
        lenient().when(coredeuxService.loadAll(argThat(list -> list != null && !list.isEmpty()), eq(DrlRuleRecord.class), eq(-1), eq(-1)))
                .thenReturn(empty);

        assertTrue(service.findByCode(" ").isEmpty());
        assertFalse(service.findByCode(null).isPresent());
    }
}
