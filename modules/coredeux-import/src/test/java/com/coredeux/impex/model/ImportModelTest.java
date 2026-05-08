package com.coredeux.impex.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    void shouldSupportImportColumnGeneratedMethods() {
        Map<String, Object> metadata = new LinkedHashMap<>(Map.of("source", "file"));
        ImportColumn column = new ImportColumn("sku", true, true, "UNKNOWN", "append", "code", "handler", metadata);
        ImportColumn same = ImportColumn.builder()
                .name("sku")
                .unique(true)
                .nullSearch(true)
                .defaultValue("UNKNOWN")
                .mode("append")
                .reference("code")
                .handler("handler")
                .metadata(metadata)
                .build();
        ImportColumn different = ImportColumn.builder().name("name").build();

        assertEquals(same, column);
        assertEquals(same.hashCode(), column.hashCode());
        assertNotEquals(different, column);
        assertNotEquals(column, null);
        assertTrue(column.toString().contains("sku"));

        column.setName("code");
        column.setUnique(false);
        column.setNullSearch(false);
        column.setDefaultValue("DEFAULT");
        column.setMode("replace");
        column.setReference("*");
        column.setHandler("otherHandler");
        column.setMetadata(new LinkedHashMap<>(Map.of("updated", true)));

        assertEquals("code", column.getName());
        assertFalse(column.isUnique());
        assertFalse(column.isNullSearch());
        assertEquals("DEFAULT", column.getDefaultValue());
        assertEquals("replace", column.getMode());
        assertEquals("*", column.getReference());
        assertEquals("otherHandler", column.getHandler());
        assertEquals(Boolean.TRUE, column.getMetadata().get("updated"));
    }

    @Test
    void shouldSupportImportLookupGeneratedMethods() {
        ImportLookup lookup = new ImportLookup("sku", "STARTSWITH", "rawSku", true,
                new LinkedHashMap<>(Map.of("case", "upper")));
        ImportLookup same = ImportLookup.builder()
                .field("sku")
                .comparator("STARTSWITH")
                .column("rawSku")
                .nullSearch(true)
                .metadata(new LinkedHashMap<>(Map.of("case", "upper")))
                .build();

        assertEquals(same, lookup);
        assertEquals(same.hashCode(), lookup.hashCode());
        assertNotEquals(ImportLookup.builder().field("name").build(), lookup);
        assertTrue(lookup.toString().contains("STARTSWITH"));

        lookup.setField("name");
        lookup.setComparator("EQUALS");
        lookup.setColumn("rawName");
        lookup.setNullSearch(false);
        lookup.setMetadata(new LinkedHashMap<>(Map.of("case", "lower")));

        assertEquals("name", lookup.getField());
        assertEquals("EQUALS", lookup.getComparator());
        assertEquals("rawName", lookup.getColumn());
        assertFalse(lookup.isNullSearch());
        assertEquals("lower", lookup.getMetadata().get("case"));
    }

    @Test
    void shouldSupportImportMacroQueryParamAndOptionsGeneratedMethods() {
        ImportMacro macro = new ImportMacro("com.example.Product", new LinkedHashMap<>(Map.of("source", "alias")));
        ImportMacro macroSame = ImportMacro.builder()
                .value("com.example.Product")
                .metadata(new LinkedHashMap<>(Map.of("source", "alias")))
                .build();
        ImportQueryParam param = new ImportQueryParam("sku", new LinkedHashMap<>(Map.of("required", true)));
        ImportQueryParam paramSame = ImportQueryParam.builder()
                .column("sku")
                .metadata(new LinkedHashMap<>(Map.of("required", true)))
                .build();
        ImportOptions options = new ImportOptions(3, true, true);
        ImportOptions optionsSame = ImportOptions.builder().passes(3).failFast(true).validateOnly(true).build();

        assertEquals(macroSame, macro);
        assertEquals(paramSame, param);
        assertEquals(optionsSame, options);
        assertEquals(macroSame.hashCode(), macro.hashCode());
        assertEquals(paramSame.hashCode(), param.hashCode());
        assertEquals(optionsSame.hashCode(), options.hashCode());
        assertTrue(macro.toString().contains("Product"));
        assertTrue(param.toString().contains("sku"));
        assertTrue(options.toString().contains("passes=3"));

        macro.setValue("Customer");
        macro.setMetadata(new LinkedHashMap<>(Map.of("source", "updated")));
        param.setColumn("name");
        param.setMetadata(new LinkedHashMap<>(Map.of("required", false)));
        options.setPasses(4);
        options.setFailFast(false);
        options.setValidateOnly(false);

        assertEquals("Customer", macro.getValue());
        assertEquals("updated", macro.getMetadata().get("source"));
        assertEquals("name", param.getColumn());
        assertEquals(Boolean.FALSE, param.getMetadata().get("required"));
        assertEquals(4, options.getPasses());
        assertFalse(options.isFailFast());
        assertFalse(options.isValidateOnly());
    }

    @Test
    void shouldSupportImportQueryRowRequestResponseAndStatementGeneratedMethods() {
        ImportColumn column = ImportColumn.builder().name("sku").build();
        ImportLookup lookup = ImportLookup.builder().field("sku").build();
        ImportQueryParam param = ImportQueryParam.builder().column("sku").build();
        ImportQuery query = new ImportQuery("sku = :sku", new LinkedHashMap<>(Map.of("sku", param)),
                new LinkedHashMap<>(Map.of("backend", "jpa")));
        ImportRow row = new ImportRow("&row", new LinkedHashMap<>(Map.of("sku", "A-1")),
                new LinkedHashMap<>(Map.of("line", 7)));
        ImportStatement statement = new ImportStatement(ImportOperation.UPSERT, "Product", new ArrayList<>(List.of(column)),
                new ArrayList<>(List.of(lookup)), query, new ArrayList<>(List.of(row)),
                new LinkedHashMap<>(Map.of("source", "test")));
        ImportRequest request = new ImportRequest(new LinkedHashMap<>(Map.of("&Product", new ImportMacro("Product",
                new LinkedHashMap<>()))), new ImportOptions(2, false, false), new ArrayList<>(List.of(statement)));
        ImportResponse response = new ImportResponse(new ArrayList<>(List.of(ImportLog.builder()
                .severity(ImportSeverity.INFO)
                .message("ok")
                .build())));

        assertEquals(new ImportQuery("sku = :sku", new LinkedHashMap<>(Map.of("sku", param)),
                new LinkedHashMap<>(Map.of("backend", "jpa"))), query);
        assertEquals(new ImportRow("&row", new LinkedHashMap<>(Map.of("sku", "A-1")),
                new LinkedHashMap<>(Map.of("line", 7))), row);
        assertEquals(request, new ImportRequest(new LinkedHashMap<>(request.getMacros()), request.getOptions(),
                new ArrayList<>(request.getStatements())));
        assertEquals(response, new ImportResponse(new ArrayList<>(response.getLogs())));
        assertEquals(statement, ImportStatement.builder()
                .operation(ImportOperation.UPSERT)
                .entity("Product")
                .columns(new ArrayList<>(List.of(column)))
                .lookup(new ArrayList<>(List.of(lookup)))
                .query(query)
                .rows(new ArrayList<>(List.of(row)))
                .metadata(new LinkedHashMap<>(Map.of("source", "test")))
                .build());
        assertTrue(statement.toString().contains("UPSERT"));

        query.setText("name = :name");
        query.setParams(new LinkedHashMap<>(Map.of("name", ImportQueryParam.builder().column("name").build())));
        query.setMetadata(new LinkedHashMap<>(Map.of("backend", "custom")));
        row.setKey("&updated");
        row.setValues(new LinkedHashMap<>(Map.of("name", "Demo")));
        row.setMetadata(new LinkedHashMap<>(Map.of("line", 8)));
        statement.setOperation(ImportOperation.MODIFY);
        statement.setEntity("Customer");
        statement.setColumns(new ArrayList<>(List.of(ImportColumn.builder().name("name").build())));
        statement.setLookup(new ArrayList<>());
        statement.setQuery(query);
        statement.setRows(new ArrayList<>(List.of(row)));
        statement.setMetadata(new LinkedHashMap<>(Map.of("source", "updated")));
        request.setMacros(new LinkedHashMap<>());
        request.setOptions(new ImportOptions(1, true, false));
        request.setStatements(new ArrayList<>(List.of(statement)));
        response.setLogs(new ArrayList<>());

        assertEquals("name = :name", query.getText());
        assertEquals("name", query.getParams().get("name").getColumn());
        assertEquals("custom", query.getMetadata().get("backend"));
        assertEquals("&updated", row.getKey());
        assertEquals("Demo", row.getValues().get("name"));
        assertEquals(8, row.getMetadata().get("line"));
        assertEquals(ImportOperation.MODIFY, statement.getOperation());
        assertEquals("Customer", statement.getEntity());
        assertEquals("updated", statement.getMetadata().get("source"));
        assertTrue(request.getMacros().isEmpty());
        assertEquals(1, request.getOptions().getPasses());
        assertTrue(request.getOptions().isFailFast());
        assertTrue(response.getLogs().isEmpty());
    }

    @Test
    void shouldDetectGeneratedEqualityDifferencesAcrossLargeModels() {
        ImportColumn column = ImportColumn.builder().name("sku").unique(true).nullSearch(true).defaultValue("A")
                .mode("append").reference("code").handler("handler").metadata(new LinkedHashMap<>(Map.of("a", 1)))
                .build();
        assertNotEquals(column, ImportColumn.builder().name("name").unique(true).nullSearch(true).defaultValue("A")
                .mode("append").reference("code").handler("handler").metadata(new LinkedHashMap<>(Map.of("a", 1)))
                .build());
        assertNotEquals(column, ImportColumn.builder().name("sku").unique(false).nullSearch(true).defaultValue("A")
                .mode("append").reference("code").handler("handler").metadata(new LinkedHashMap<>(Map.of("a", 1)))
                .build());
        assertNotEquals(column, ImportColumn.builder().name("sku").unique(true).nullSearch(false).defaultValue("A")
                .mode("append").reference("code").handler("handler").metadata(new LinkedHashMap<>(Map.of("a", 1)))
                .build());
        assertNotEquals(column, ImportColumn.builder().name("sku").unique(true).nullSearch(true).defaultValue("B")
                .mode("append").reference("code").handler("handler").metadata(new LinkedHashMap<>(Map.of("a", 1)))
                .build());
        assertNotEquals(column, ImportColumn.builder().name("sku").unique(true).nullSearch(true).defaultValue("A")
                .mode("replace").reference("code").handler("handler").metadata(new LinkedHashMap<>(Map.of("a", 1)))
                .build());
        assertNotEquals(column, ImportColumn.builder().name("sku").unique(true).nullSearch(true).defaultValue("A")
                .mode("append").reference("*").handler("handler").metadata(new LinkedHashMap<>(Map.of("a", 1)))
                .build());
        assertNotEquals(column, ImportColumn.builder().name("sku").unique(true).nullSearch(true).defaultValue("A")
                .mode("append").reference("code").handler("other").metadata(new LinkedHashMap<>(Map.of("a", 1)))
                .build());
        assertNotEquals(column, ImportColumn.builder().name("sku").unique(true).nullSearch(true).defaultValue("A")
                .mode("append").reference("code").handler("handler").metadata(new LinkedHashMap<>(Map.of("b", 2)))
                .build());

        ImportStatement statement = ImportStatement.builder()
                .operation(ImportOperation.UPSERT)
                .entity("Product")
                .columns(new ArrayList<>(List.of(column)))
                .lookup(new ArrayList<>(List.of(ImportLookup.builder().field("sku").build())))
                .query(ImportQuery.builder().text("sku = :sku").params(new LinkedHashMap<>(Map.of("sku",
                        ImportQueryParam.builder().column("sku").build()))).build())
                .rows(new ArrayList<>(List.of(ImportRow.builder().key("&row").build())))
                .metadata(new LinkedHashMap<>(Map.of("source", "test")))
                .build();
        assertNotEquals(statement, ImportStatement.builder()
                .operation(ImportOperation.CREATE)
                .entity("Product")
                .columns(new ArrayList<>(List.of(column)))
                .lookup(new ArrayList<>(List.of(ImportLookup.builder().field("sku").build())))
                .query(statement.getQuery())
                .rows(new ArrayList<>(statement.getRows()))
                .metadata(new LinkedHashMap<>(statement.getMetadata()))
                .build());
        assertNotEquals(statement, ImportStatement.builder()
                .operation(ImportOperation.UPSERT)
                .entity("Customer")
                .columns(new ArrayList<>(List.of(column)))
                .lookup(new ArrayList<>(statement.getLookup()))
                .query(statement.getQuery())
                .rows(new ArrayList<>(statement.getRows()))
                .metadata(new LinkedHashMap<>(statement.getMetadata()))
                .build());
        assertNotEquals(statement, ImportStatement.builder()
                .operation(ImportOperation.UPSERT)
                .entity("Product")
                .columns(new ArrayList<>())
                .lookup(new ArrayList<>(statement.getLookup()))
                .query(statement.getQuery())
                .rows(new ArrayList<>(statement.getRows()))
                .metadata(new LinkedHashMap<>(statement.getMetadata()))
                .build());
        assertNotEquals(statement, ImportStatement.builder()
                .operation(ImportOperation.UPSERT)
                .entity("Product")
                .columns(new ArrayList<>(List.of(column)))
                .lookup(new ArrayList<>())
                .query(statement.getQuery())
                .rows(new ArrayList<>(statement.getRows()))
                .metadata(new LinkedHashMap<>(statement.getMetadata()))
                .build());
        assertNotEquals(statement, ImportStatement.builder()
                .operation(ImportOperation.UPSERT)
                .entity("Product")
                .columns(new ArrayList<>(List.of(column)))
                .lookup(new ArrayList<>(statement.getLookup()))
                .query(null)
                .rows(new ArrayList<>(statement.getRows()))
                .metadata(new LinkedHashMap<>(statement.getMetadata()))
                .build());
        assertNotEquals(statement, ImportStatement.builder()
                .operation(ImportOperation.UPSERT)
                .entity("Product")
                .columns(new ArrayList<>(List.of(column)))
                .lookup(new ArrayList<>(statement.getLookup()))
                .query(statement.getQuery())
                .rows(new ArrayList<>())
                .metadata(new LinkedHashMap<>(statement.getMetadata()))
                .build());
        assertNotEquals(statement, ImportStatement.builder()
                .operation(ImportOperation.UPSERT)
                .entity("Product")
                .columns(new ArrayList<>(List.of(column)))
                .lookup(new ArrayList<>(statement.getLookup()))
                .query(statement.getQuery())
                .rows(new ArrayList<>(statement.getRows()))
                .metadata(new LinkedHashMap<>(Map.of("source", "other")))
                .build());
    }
}
