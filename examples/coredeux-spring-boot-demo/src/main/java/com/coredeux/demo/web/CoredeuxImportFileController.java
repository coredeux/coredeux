package com.coredeux.demo.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.parser.excel.CoredeuxExcelImportParser;
import com.coredeux.impex.parser.exception.CoredeuxImportParserException;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;
import com.coredeux.impex.service.CoredeuxImportService;
import com.coredeux.spring.boot.autoconfigure.CoredeuxImportProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/import/file")
@Tag(name = "Coredeux Demo File Import", description = "Text and Excel import file endpoints backed by Coredeux import parser")
public class CoredeuxImportFileController {

    private static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CoredeuxImportService coredeuxImportService;
    private final CoredeuxTextImportParser textImportParser;
    private final CoredeuxExcelImportParser excelImportParser;
    private final CoredeuxImportProperties coredeuxImportProperties;

    public CoredeuxImportFileController(CoredeuxImportService coredeuxImportService,
            CoredeuxTextImportParser textImportParser, CoredeuxExcelImportParser excelImportParser,
            CoredeuxImportProperties coredeuxImportProperties) {
        this.coredeuxImportService = coredeuxImportService;
        this.textImportParser = textImportParser;
        this.excelImportParser = excelImportParser;
        this.coredeuxImportProperties = coredeuxImportProperties;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Execute an import file")
    public ResponseEntity<ImportResponse> importFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName) throws IOException {
        return response(coredeuxImportService.importData(parseFile(file, sheetName)));
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate an import file without persisting data")
    public ResponseEntity<ImportResponse> validateFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName) throws IOException {
        return response(coredeuxImportService.validateData(parseFile(file, sheetName)));
    }

    /**
     * Selects the parser from the uploaded file metadata and compiles the file into
     * an import request.
     */
    private ImportRequest parseFile(MultipartFile file, String sheetName) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new CoredeuxImportParserException("Import file must not be empty");
        }
        if (isExcel(file) || "excel".equalsIgnoreCase(defaultParser())) {
            try (InputStream inputStream = file.getInputStream()) {
                return excelImportParser.parse(inputStream, sheetName);
            }
        }
        return textImportParser.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
    }

    /**
     * Detects Excel uploads by content type first and file extension second.
     */
    private boolean isExcel(MultipartFile file) {
        String contentType = normalize(file.getContentType());
        if (XLS_CONTENT_TYPE.equals(contentType) || XLSX_CONTENT_TYPE.equals(contentType)) {
            return true;
        }
        String filename = normalize(file.getOriginalFilename());
        return filename.endsWith(".xls") || filename.endsWith(".xlsx");
    }

    /**
     * Normalizes optional upload metadata for case-insensitive comparisons.
     */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Reads the configured fallback parser from Spring Boot properties first, then
     * falls back to the Coredeux native defaults. Text stays the safe default when
     * the property is missing or blank.
     */
    private String defaultParser() {
        String parser = coredeuxImportProperties == null ? null : coredeuxImportProperties.defaultParser();
        return parser == null || parser.isBlank() ? "text" : parser.trim();
    }

    /**
     * Mirrors the JSON import endpoint response contract.
     */
    private ResponseEntity<ImportResponse> response(ImportResponse response) {
        HttpStatus status = response != null && response.hasErrors() ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
