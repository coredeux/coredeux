package com.coredeux.demo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.parser.excel.CoredeuxExcelImportParser;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;

class CoredeuxImportFileSampleTest {

    @Test
    void shouldParsePostmanTextSample() throws Exception {
        ImportRequest request = new CoredeuxTextImportParser()
                .parse(Files.readString(Path.of("postman/samples/import-products.import")));

        assertEquals("com.coredeux.demo.domain.Product", request.getStatements().get(0).getEntity());
        assertEquals("FILE-TEXT-1001", request.getStatements().get(0).getRows().get(0).getValues().get("sku"));
    }

    @Test
    void shouldParsePostmanExcelSample() throws Exception {
        try (InputStream inputStream = Files.newInputStream(Path.of("postman/samples/import-products.xlsx"))) {
            ImportRequest request = new CoredeuxExcelImportParser().parse(inputStream, "Products");

            assertEquals("com.coredeux.demo.domain.Product", request.getStatements().get(0).getEntity());
            assertEquals("FILE-XLSX-1001", request.getStatements().get(0).getRows().get(0).getValues().get("sku"));
        }
    }
}
