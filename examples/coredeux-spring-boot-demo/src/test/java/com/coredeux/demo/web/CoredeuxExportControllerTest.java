package com.coredeux.demo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.coredeux.demo.export.DatabaseCoredeuxExportStorageService;
import com.coredeux.demo.export.ExportStorageRecord;
import com.coredeux.export.model.ExportFormat;
import com.coredeux.export.model.ExportRequest;
import com.coredeux.export.model.ExportResponse;
import com.coredeux.export.model.ExportStatus;
import com.coredeux.export.service.CoredeuxExportService;

@WebMvcTest(CoredeuxExportController.class)
class CoredeuxExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoredeuxExportService exportService;

    @MockBean
    private DatabaseCoredeuxExportStorageService storageService;

    @Test
    void shouldQueueExportRequest() throws Exception {
        when(exportService.queueExport(any(ExportRequest.class))).thenReturn(ExportResponse.builder()
                .uid("exp-123")
                .status(ExportStatus.NEW)
                .format(ExportFormat.TEXT)
                .build());

        mockMvc.perform(post("/api/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entity": "com.coredeux.demo.domain.Product",
                                  "fieldList": [
                                    { "path": "sku" }
                                  ]
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.uid").value("exp-123"))
                .andExpect(jsonPath("$.status").value("NEW"));

        verify(exportService).queueExport(any(ExportRequest.class));
    }

    @Test
    void shouldReturnExportStatus() throws Exception {
        when(exportService.getExport("exp-123")).thenReturn(ExportResponse.builder()
                .uid("exp-123")
                .status(ExportStatus.COMPLETED)
                .build());

        mockMvc.perform(get("/api/export/exp-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldDownloadStoredExportFromDatabase() throws Exception {
        when(storageService.findByUid("exp-123")).thenReturn(Optional.of(ExportStorageRecord.builder()
                .uid("exp-123")
                .fileName("customers.txt")
                .storageType("database")
                .contentType("text/plain;charset=UTF-8")
                .size(11L)
                .content("hello world".getBytes(StandardCharsets.UTF_8))
                .metadataJson("{}")
                .url("/api/export/exp-123/download")
                .canonicalUrl("/api/export/exp-123/download")
                .build()));

        mockMvc.perform(get("/api/export/exp-123/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"customers.txt\""))
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("hello world"));
    }
}
