package com.coredeux.demo.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coredeux.demo.domain.DrlRuleRecord;
import com.coredeux.demo.drl.DemoDrlRuleSourceService;
import com.coredeux.drl.service.DRLService;

@ExtendWith(MockitoExtension.class)
class DrlRuleControllerTest {

    @Mock
    private DemoDrlRuleSourceService ruleSourceService;

    @Mock
    private DRLService drlService;

    @Test
    void listsGetsConvertsDeletesAndPurgesRules() {
        DrlRuleController controller = new DrlRuleController(ruleSourceService, drlService);
        DrlRuleRecord record = new DrlRuleRecord();
        record.setPk(1L);
        record.setCode("customerValidator.drl");
        record.setDrl("rule \"validate\"");

        when(ruleSourceService.findAll()).thenReturn(List.of(record));
        when(ruleSourceService.findByCode("customerValidator.drl")).thenReturn(java.util.Optional.of(record));
        when(ruleSourceService.convertAndSave("source")).thenReturn(record);

        assertEquals(1, controller.list().size());
        assertNotNull(controller.get("customerValidator.drl").getBody());
        assertEquals("rule \"validate\"", controller.get("customerValidator.drl").getBody());

        DrlRuleRecord converted = controller.convert("source");
        assertEquals("customerValidator.drl", converted.getCode());
        verify(ruleSourceService).convertAndSave("source");
        verify(drlService).compileAndCache(record.getCode());

        assertEquals(204, controller.delete("customerValidator.drl").getStatusCode().value());
        verify(ruleSourceService).delete("customerValidator.drl");
        verify(drlService).purgeCache("customerValidator.drl");

        assertEquals(204, controller.purgeRuleCache("customerValidator.drl").getStatusCode().value());
        verify(drlService, times(2)).purgeCache("customerValidator.drl");

        assertEquals(204, controller.purgeAllCache().getStatusCode().value());
        verify(drlService).purgeCache();
    }

    @Test
    void returnsNotFoundWhenRuleDoesNotExist() {
        DrlRuleController controller = new DrlRuleController(ruleSourceService, drlService);
        when(ruleSourceService.findByCode("missing")).thenReturn(java.util.Optional.empty());

        assertNull(controller.get("missing").getBody());
        assertEquals(404, controller.get("missing").getStatusCode().value());
    }
}
