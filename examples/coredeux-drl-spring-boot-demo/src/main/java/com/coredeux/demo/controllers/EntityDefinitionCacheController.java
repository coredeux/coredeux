package com.coredeux.demo.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coredeux.demo.definition.EntityDefinitionManager;

@RestController
@RequestMapping("/api/drl/entity-definitions/cache")
public class EntityDefinitionCacheController {

	private final EntityDefinitionManager manager;

	public EntityDefinitionCacheController(EntityDefinitionManager manager) {
		this.manager = manager;
	}

	@GetMapping
	public Map<String, Object> status() {
		Map<String, Object> response = new HashMap<>();
		response.put("cached", manager.isCached());
		response.put("records", manager.findRecord().map(record -> 1).orElse(0));
		response.put("registryCode", manager.findRecord().map(record -> record.getCode()).orElse(""));
		return response;
	}

	@GetMapping("/yml")
	public String getYML() {
		return manager.getCachedYML();
	}

	@PostMapping("/refresh")
	public Map<String, Object> refresh() {
		manager.refreshCache();
		return Map.of("refreshed", true, "cached", manager.isCached());
	}

	@PostMapping("/bootstrap")
	public Map<String, Object> bootstrap() {
		return Map.of("bootstrapped", true, "cached", manager.updateEntityDefinitionFromFile().getCode() != null);
	}

	@PutMapping
	public Map<String, Object> update(@RequestBody String yaml,
			@RequestParam(name = "sourceLocation", defaultValue = "postman") String sourceLocation) {
		var saved = manager.updateDefinition(yaml, sourceLocation);
		Map<String, Object> response = new HashMap<>();
		response.put("updated", true);
		response.put("cached", manager.isCached());
		response.put("code", saved.getCode());
		response.put("sourceLocation", saved.getSourceLocation());
		response.put("updatedAt", saved.getUpdatedAt());
		return response;
	}

	@DeleteMapping
	public ResponseEntity<Void> purge() {
		manager.purgeCache();
		return ResponseEntity.noContent().build();
	}
}
