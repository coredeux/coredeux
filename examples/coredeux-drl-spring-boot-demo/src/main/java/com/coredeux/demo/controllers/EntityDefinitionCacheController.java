package com.coredeux.demo.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coredeux.demo.definition.DatabaseBackedEntityDefinitionRegistry;

@RestController
@RequestMapping("/api/drl/entity-definitions/cache")
public class EntityDefinitionCacheController {

    private final DatabaseBackedEntityDefinitionRegistry registry;

    public EntityDefinitionCacheController(DatabaseBackedEntityDefinitionRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public Map<String, Object> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("cached", registry.isCached());
        response.put("records", registry.findRecord().map(record -> 1).orElse(0));
        response.put("registryCode", registry.findRecord().map(record -> record.getCode()).orElse(""));
        return response;
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        registry.refreshCache();
        return Map.of("refreshed", true, "cached", registry.isCached());
    }

    @PostMapping("/bootstrap")
    public Map<String, Object> bootstrap() {
        return Map.of("bootstrapped", true, "cached", registry.bootstrapFromFile().getCode() != null);
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody String yaml,
            @RequestParam(name = "sourceLocation", defaultValue = "postman") String sourceLocation) {
        var saved = registry.updateDefinition(yaml, sourceLocation);
        Map<String, Object> response = new HashMap<>();
        response.put("updated", true);
        response.put("cached", registry.isCached());
        response.put("code", saved.getCode());
        response.put("sourceLocation", saved.getSourceLocation());
        response.put("updatedAt", saved.getUpdatedAt());
        return response;
    }

    @DeleteMapping
    public ResponseEntity<Void> purge() {
        registry.purgeCache();
        return ResponseEntity.noContent().build();
    }
}
