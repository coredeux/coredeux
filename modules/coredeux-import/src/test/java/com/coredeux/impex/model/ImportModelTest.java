package com.coredeux.impex.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ImportModelTest {

    @Test
    void shouldInitializeBuilderDefaults() {
        ImportColumn column = ImportColumn.builder().name("sku").build();
        ImportLookup lookup = ImportLookup.builder().field("sku").build();
        ImportMacro macro = ImportMacro.builder().value("Product").build();
        ImportQuery query = ImportQuery.builder().text("sku = :sku").build();
        ImportQueryParam queryParam = ImportQueryParam.builder().column("sku").build();
        ImportRequest request = ImportRequest.builder().build();
        ImportResponse response = ImportResponse.builder().build();
        ImportRow row = ImportRow.builder().key("&row").build();
        ImportStatement statement = ImportStatement.builder().entity("Product").build();

        assertNotNull(column.getMetadata());
        assertNotNull(lookup.getMetadata());
        assertNotNull(macro.getMetadata());
        assertNotNull(query.getParams());
        assertNotNull(query.getMetadata());
        assertNotNull(queryParam.getMetadata());
        assertNotNull(request.getOptions());
        assertNotNull(request.getMacros());
        assertNotNull(request.getStatements());
        assertNotNull(response.getLogs());
        assertNotNull(row.getValues());
        assertNotNull(row.getMetadata());
        assertNotNull(statement.getColumns());
        assertNotNull(statement.getRows());
        assertNotNull(statement.getLookup());
        assertNotNull(statement.getMetadata());
    }

    @Test
    void shouldReportErrorsForErrorAndCriticalSeverities() {
        assertFalse(new ImportResponse(List.of()).hasErrors());
        assertFalse(new ImportResponse(List.of(ImportLog.builder().severity(ImportSeverity.INFO).build())).hasErrors());
        assertTrue(new ImportResponse(List.of(ImportLog.builder().severity(ImportSeverity.ERROR).build())).hasErrors());
        assertTrue(new ImportResponse(List.of(ImportLog.builder().severity(ImportSeverity.CRITICAL).build())).hasErrors());
    }

    @Test
    void shouldSupportGeneratedAccessorsEqualityAndToString() {
        ImportLog log = new ImportLog(ImportSeverity.WARN, "message", 1, 2, "Product", "sku", "IllegalStateException");
        ImportLog same = ImportLog.builder()
                .severity(ImportSeverity.WARN)
                .message("message")
                .statementIndex(1)
                .rowIndex(2)
                .entity("Product")
                .column("sku")
                .exceptionType("IllegalStateException")
                .build();
        ImportLog different = ImportLog.builder().severity(ImportSeverity.ERROR).message("message").build();

        assertEquals(same, log);
        assertEquals(same.hashCode(), log.hashCode());
        assertNotEquals(different, log);
        assertTrue(log.toString().contains("message"));
        assertEquals("sku", log.getColumn());

        log.setColumn("name");
        assertEquals("name", log.getColumn());
    }
}
