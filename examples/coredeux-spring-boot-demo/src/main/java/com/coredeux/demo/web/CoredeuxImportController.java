package com.coredeux.demo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.service.CoredeuxImportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/import")
@Tag(name = "Coredeux Demo Import", description = "JSON import endpoints backed by CoredeuxImportService")
public class CoredeuxImportController {

    private final CoredeuxImportService coredeuxImportService;

    public CoredeuxImportController(CoredeuxImportService coredeuxImportService) {
        this.coredeuxImportService = coredeuxImportService;
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate an import JSON request without persisting data")
    public ResponseEntity<ImportResponse> validate(@RequestBody ImportRequest request) {
        return response(coredeuxImportService.validateData(request));
    }

    @PostMapping
    @Operation(summary = "Execute an import JSON request")
    public ResponseEntity<ImportResponse> importData(@RequestBody ImportRequest request) {
        return response(coredeuxImportService.importData(request));
    }

    private ResponseEntity<ImportResponse> response(ImportResponse response) {
        HttpStatus status = response != null && response.hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
