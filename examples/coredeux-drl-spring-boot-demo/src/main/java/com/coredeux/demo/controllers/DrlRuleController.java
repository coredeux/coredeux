package com.coredeux.demo.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coredeux.demo.domain.DrlRuleRecord;
import com.coredeux.demo.drl.DemoDrlRuleSourceService;
import com.coredeux.drl.service.DRLService;

@RestController
@RequestMapping("/api/drl/rules")
public class DrlRuleController {

    private final DemoDrlRuleSourceService ruleSourceService;
    private final DRLService drlService;

    public DrlRuleController(DemoDrlRuleSourceService ruleSourceService, DRLService drlService) {
        this.ruleSourceService = ruleSourceService;
        this.drlService = drlService;
    }

    @GetMapping
    public List<DrlRuleRecord> list() {
        return ruleSourceService.findAll();
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<String> get(@PathVariable("ruleId") String ruleId) {
        return ruleSourceService.findByCode(ruleId)
                .map(n -> ResponseEntity.ok(n.getDrl()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/convert")
    public DrlRuleRecord convert(@RequestBody String source) {
    	DrlRuleRecord record = ruleSourceService.convertAndSave(source);
    	drlService.compileAndCache(record.getCode());
        return record;
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> delete(@PathVariable("ruleId") String ruleId) {
        ruleSourceService.delete(ruleId);
        drlService.purgeCache(ruleId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/cache/{ruleId}")
    public ResponseEntity<Void> purgeRuleCache(@PathVariable("ruleId") String ruleId) {
        drlService.purgeCache(ruleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> purgeAllCache() {
        drlService.purgeCache();
        return ResponseEntity.noContent().build();
    }
}
