package com.coredeux.demo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.parser.excel.CoredeuxExcelImportParser;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;

class CoredeuxImportFileSampleTest {

    @Test
    void shouldParseTextSamples() throws Exception {
        Map<String, String> samples = Map.of(
                "import-products.import", "com.coredeux.demo.domain.Product",
                "import-jdbc-inventory.import", "com.coredeux.demo.domain.jdbc.JdbcInventoryItem",
                "import-mongodb-audit-trails.import", "com.coredeux.demo.domain.mongodb.MongoAuditTrail",
                "import-elasticsearch-catalog.import",
                "com.coredeux.demo.domain.elasticsearch.ElasticsearchCatalogEntry",
                "import-redis-sessions.import", "com.coredeux.demo.domain.redis.RedisSessionSnapshot");

        CoredeuxTextImportParser parser = new CoredeuxTextImportParser();
        for (Map.Entry<String, String> sample : samples.entrySet()) {
            ImportRequest request = parser.parse(Files.readString(Path.of("samples", sample.getKey())));

            assertEquals(sample.getValue(), request.getStatements().get(0).getEntity());
        }
    }

    @Test
    void shouldParseExcelSample() throws Exception {
        try (InputStream inputStream = Files.newInputStream(Path.of("samples/import-products.xlsx"))) {
            ImportRequest request = new CoredeuxExcelImportParser().parse(inputStream, "Products");

            assertEquals("com.coredeux.demo.domain.Product", request.getStatements().get(0).getEntity());
            assertEquals("FILE-XLSX-1001", request.getStatements().get(0).getRows().get(0).getValues().get("sku"));
        }
    }
}
