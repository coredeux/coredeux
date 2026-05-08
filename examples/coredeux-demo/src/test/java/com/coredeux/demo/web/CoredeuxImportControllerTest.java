package com.coredeux.demo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.coredeux.impex.model.ImportLog;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.model.ImportSeverity;
import com.coredeux.impex.service.CoredeuxImportService;

@WebMvcTest(CoredeuxImportController.class)
@Import({CoredeuxDemoExceptionHandler.class, CoredeuxImportControllerTest.TestConfig.class})
class CoredeuxImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoredeuxImportService coredeuxImportService;

    @Test
    void shouldValidateImportJson() throws Exception {
        when(coredeuxImportService.validateData(any(ImportRequest.class)))
                .thenReturn(ImportResponse.builder().build());

        mockMvc.perform(post("/api/import/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statements\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray());

        verify(coredeuxImportService).validateData(any(ImportRequest.class));
    }

    @Test
    void shouldImportJson() throws Exception {
        when(coredeuxImportService.importData(any(ImportRequest.class)))
                .thenReturn(ImportResponse.builder().build());

        mockMvc.perform(post("/api/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statements\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.references").doesNotExist());

        verify(coredeuxImportService).importData(any(ImportRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenImportResponseHasErrors() throws Exception {
        ImportResponse response = ImportResponse.builder()
                .logs(List.of(ImportLog.builder()
                        .severity(ImportSeverity.ERROR)
                        .message("Column 'missing' maps to missing field")
                        .build()))
                .build();
        when(coredeuxImportService.importData(any(ImportRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statements\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.logs[0].message").value("Column 'missing' maps to missing field"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        }
    }
}
