package com.coredeux.demo.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/entities/{entityName}")
@Tag(name = "Coredeux Demo CRUD", description = "Generic CRUD endpoints backed by CoredeuxService")
public class CoredeuxCrudController {

    private final CoredeuxService coredeuxService;
    private final DemoEntityResolver demoEntityResolver;
    private final ObjectMapper objectMapper;

    public CoredeuxCrudController(CoredeuxService coredeuxService, DemoEntityResolver demoEntityResolver,
            ObjectMapper objectMapper) {
        this.coredeuxService = coredeuxService;
        this.demoEntityResolver = demoEntityResolver;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new entity instance")
    public ResponseEntity<?> create(
            @Parameter(description = "Fully qualified class name of the entity")
            @PathVariable("entityName") String entityName,
            @RequestBody Map<String, Object> payload) {
        demoEntityResolver.resolveDefinition(entityName);
        Class<?> entityType = demoEntityResolver.resolveType(entityName);
        Object entity = objectMapper.convertValue(payload, entityType);
        String identifier = coredeuxService.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(coredeuxService.load(identifier, entityType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read an entity by identifier")
    public ResponseEntity<?> read(
            @Parameter(description = "Fully qualified class name of the entity")
            @PathVariable("entityName") String entityName,
            @Parameter(description = "Entity identifier value")
            @PathVariable("id") String id) {
        Class<?> entityType = demoEntityResolver.resolveType(entityName);
        return ResponseEntity.ok(coredeuxService.load(id, entityType));
    }

    @GetMapping
    @Operation(summary = "List entities with pagination")
    public ResponseEntity<?> list(
            @Parameter(description = "Fully qualified class name of the entity")
            @PathVariable("entityName") String entityName,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "currentPage", defaultValue = "1") int currentPage) {
        Class<?> entityType = demoEntityResolver.resolveType(entityName);
        SearchResult<?> result = coredeuxService.loadAll(List.of(), entityType, pageSize, currentPage);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing entity")
    public ResponseEntity<?> update(
            @Parameter(description = "Fully qualified class name of the entity")
            @PathVariable("entityName") String entityName,
            @Parameter(description = "Entity identifier value")
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> payload) {
        CoredeuxEntityDefinition definition = demoEntityResolver.resolveDefinition(entityName);
        Class<?> entityType = demoEntityResolver.resolveType(entityName);
        Object entity = objectMapper.convertValue(payload, entityType);
        Object typedIdentifier = objectMapper.convertValue(id,
                demoEntityResolver.resolveIdentifierType(entityType, definition));
        demoEntityResolver.applyIdentifier(entity, definition, typedIdentifier);
        coredeuxService.update(entity);
        return ResponseEntity.ok(coredeuxService.load(id, entityType));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an entity by identifier")
    public ResponseEntity<?> delete(
            @Parameter(description = "Fully qualified class name of the entity")
            @PathVariable("entityName") String entityName,
            @Parameter(description = "Entity identifier value")
            @PathVariable("id") String id) {
        Class<?> entityType = demoEntityResolver.resolveType(entityName);
        coredeuxService.remove(id, entityType);
        return ResponseEntity.noContent().build();
    }
}
