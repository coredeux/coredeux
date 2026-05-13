package com.coredeux.demo.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coredeux.demo.export.DatabaseCoredeuxExportStorageService;
import com.coredeux.demo.export.ExportStorageRecord;
import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;
import com.coredeux.export.model.ExportStatus;
import com.coredeux.export.service.CoredeuxExportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/export")
@Tag(name = "Coredeux Demo Export", description = "Export endpoints backed by CoredeuxExportService")
public class CoredeuxExportController {

    private final CoredeuxExportService exportService;
    private final DatabaseCoredeuxExportStorageService storageService;

    public CoredeuxExportController(CoredeuxExportService exportService,
            DatabaseCoredeuxExportStorageService storageService) {
        this.exportService = exportService;
        this.storageService = storageService;
    }

    @PostMapping
    @Operation(summary = "Queue an export request")
    public ResponseEntity<ExportResponse> queue(@RequestBody ExportRequest request) {
        return response(exportService.queueExport(request));
    }

    @GetMapping("/{uid}")
    @Operation(summary = "Read export status by uid")
    public ResponseEntity<ExportResponse> status(@PathVariable("uid") String uid) {
        return response(exportService.getExport(uid));
    }

    @GetMapping("/{uid}/download")
    @Operation(summary = "Download the stored export file from the database")
    public ResponseEntity<byte[]> download(@PathVariable("uid") String uid) {
        ExportStorageRecord record = storageService.findByUid(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Export record not found for uid: " + uid));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(record.getContentType()));
        headers.setContentLength(record.getSize());
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + record.getFileName() + "\"");
        return new ResponseEntity<>(record.getContent(), headers, HttpStatus.OK);
    }

    private ResponseEntity<ExportResponse> response(ExportResponse response) {
        HttpStatus status = response != null && (ExportStatus.NEW.equals(response.getStatus())
                || ExportStatus.IN_PROGRESS.equals(response.getStatus()))
                        ? HttpStatus.ACCEPTED
                        : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
