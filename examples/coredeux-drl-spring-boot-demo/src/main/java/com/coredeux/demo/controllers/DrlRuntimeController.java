package com.coredeux.demo.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coredeux.demo.drl.DrlSourceExecutionRequest;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;

@RestController
@RequestMapping("/api/drl")
public class DrlRuntimeController {

    private final DRLService drlService;

    public DrlRuntimeController(DRLService drlService) {
        this.drlService = drlService;
    }

    @PostMapping("/rules/{ruleId}/execute")
    public RuleContext<?> execute(@PathVariable("ruleId") String ruleId,
            @RequestBody RuleContext<?> context) {
        drlService.execute(ruleId, context);
        return context;
    }

    @PostMapping("/rules/{ruleId}/execute-source")
    public RuleContext<?> executeWithSource(@PathVariable("ruleId") String ruleId,
            @RequestBody DrlSourceExecutionRequest request) {
        RuleContext<?> context = request == null ? null : request.getContext();
        drlService.execute(ruleId, request == null ? null : request.getSource(), context);
        return context;
    }

    @PostMapping("/execute-source")
    public RuleContext<?> executeSource(@RequestBody DrlSourceExecutionRequest request) {
        RuleContext<?> context = request == null ? null : request.getContext();
        drlService.executeSource(request == null ? null : request.getSource(), context);
        return context;
    }

    @GetMapping("/cache/{ruleId}")
    public Map<String, Object> cacheStatus(@PathVariable("ruleId") String ruleId) {
        return Map.of("ruleId", ruleId, "cached", drlService.isCached(ruleId));
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
