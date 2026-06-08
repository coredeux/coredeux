package com.coredeux.demo.drl;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drl/rules")
public class DrlRuleController {

    private final DemoDrlRuleSourceService ruleSourceService;

    public DrlRuleController(DemoDrlRuleSourceService ruleSourceService) {
        this.ruleSourceService = ruleSourceService;
    }

    @GetMapping
    public List<DrlRuleRecord> list() {
        return ruleSourceService.findAll();
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<DrlRuleRecord> get(@PathVariable("ruleId") String ruleId) {
        return ruleSourceService.findByCode(ruleId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{ruleId}")
    public DrlRuleRecord upsert(@PathVariable("ruleId") String ruleId, @RequestBody DrlRuleUpsertRequest request) {
        return ruleSourceService.save(ruleId, request == null ? null : request.getDescription(),
                request == null ? null : request.getDrl());
    }

    @PostMapping("/convert")
    public DrlRuleRecord convert(@RequestBody DrlConversionRequest request) {
        return ruleSourceService.convertAndSave(request == null ? null : request.getSource());
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> delete(@PathVariable("ruleId") String ruleId) {
        ruleSourceService.delete(ruleId);
        return ResponseEntity.noContent().build();
    }
}
