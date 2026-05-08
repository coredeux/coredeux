package com.coredeux.demo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.service.CoredeuxImportService;

@WebMvcTest(CoredeuxImportFileController.class)
@Import({CoredeuxDemoExceptionHandler.class, CoredeuxImportFileControllerTest.TestConfig.class})
class CoredeuxImportFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoredeuxImportService coredeuxImportService;

    @Test
    void shouldImportTextFile() throws Exception {
        when(coredeuxImportService.importData(any(ImportRequest.class)))
                .thenReturn(ImportResponse.builder().build());

        mockMvc.perform(multipart("/api/import/file")
                        .file(textFile("""
                                CREATE com.example.Product | sku | name
                                                           | T-1 | Text Product
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray());

        ArgumentCaptor<ImportRequest> requestCaptor = ArgumentCaptor.forClass(ImportRequest.class);
        verify(coredeuxImportService).importData(requestCaptor.capture());
        ImportRequest request = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("com.example.Product",
                request.getStatements().get(0).getEntity());
        org.junit.jupiter.api.Assertions.assertEquals("T-1",
                request.getStatements().get(0).getRows().get(0).getValues().get("sku"));
    }

    @Test
    void shouldValidateExcelFileUsingNamedSheet() throws Exception {
        when(coredeuxImportService.validateData(any(ImportRequest.class)))
                .thenReturn(ImportResponse.builder().build());

        mockMvc.perform(multipart("/api/import/file/validate")
                        .file(excelFile())
                        .param("sheetName", "Products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray());

        ArgumentCaptor<ImportRequest> requestCaptor = ArgumentCaptor.forClass(ImportRequest.class);
        verify(coredeuxImportService).validateData(requestCaptor.capture());
        ImportRequest request = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("com.example.Product",
                request.getStatements().get(0).getEntity());
        org.junit.jupiter.api.Assertions.assertEquals("X-1",
                request.getStatements().get(0).getRows().get(0).getValues().get("sku"));
    }

    @Test
    void shouldRejectEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.import", MediaType.TEXT_PLAIN_VALUE,
                new byte[0]);

        mockMvc.perform(multipart("/api/import/file").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Import file must not be empty"));
    }

    private MockMultipartFile textFile(String text) {
        return new MockMultipartFile("file", "products.import", MediaType.TEXT_PLAIN_VALUE, text.getBytes());
    }

    private MockMultipartFile excelFile() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet ignored = workbook.createSheet("Ignored");
            strings(ignored.createRow(0), "CREATE com.example.Ignored", "code");
            strings(ignored.createRow(1), "", "I-1");

            Sheet products = workbook.createSheet("Products");
            strings(products.createRow(0), "CREATE com.example.Product", "sku", "name");
            strings(products.createRow(1), "", "X-1", "Excel Product");
            workbook.write(output);
            return new MockMultipartFile("file", "products.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private void strings(Row row, String... values) {
        for (int index = 0; index < values.length; index++) {
            row.createCell(index, CellType.STRING).setCellValue(values[index]);
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        }
    }
}
