package com.coredeux.impex.parser.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.model.ImportLookup;
import com.coredeux.impex.model.ImportOperation;
import com.coredeux.impex.model.ImportQuery;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportStatement;

class CoredeuxTextImportParserGuideSampleTest {

    private final CoredeuxTextImportParser parser = new CoredeuxTextImportParser();

    @Test
    void shouldParseGuideSampleWithLookupQueryReferencesCollectionsAndMetadata() throws Exception {
        ImportRequest request = parser.parse(readSample());

        assertEquals(2, request.getOptions().getPasses());
        assertTrue(request.getOptions().isFailFast());
        assertTrue(request.getOptions().isValidateOnly());
        assertEquals("com.example.Category", request.getMacros().get("&Category").getValue());
        assertEquals("seed", request.getMacros().get("&Category").getMetadata().get("source"));
        assertEquals(1, request.getMacros().get("&Category").getMetadata().get("version"));
        assertEquals("com.example.Product", request.getMacros().get("&Product").getValue());
        assertEquals(12, request.getStatements().size());

        assertCategoryCreate(request.getStatements().get(0));
        assertProductUniqueUpsert(request.getStatements().get(1));
        assertCompoundUniqueReference(request.getStatements().get(2));
        assertLookupShorthand(request.getStatements().get(3));
        assertIndexedLookupWithSourceColumn(request.getStatements().get(4));
        assertColumnLevelLookup(request.getStatements().get(5));
        assertQueryParams(request.getStatements().get(6));
        assertExplicitQueryParam(request.getStatements().get(7));
        assertDeleteLookup(request.getStatements().get(8));
        assertFetchUniqueWithRowKey(request.getStatements().get(9));
        assertCollectionModes(request.getStatements().get(10));
        assertRowKeyReference(request.getStatements().get(11));
    }

    private void assertCategoryCreate(ImportStatement statement) {
        assertEquals(ImportOperation.CREATE, statement.getOperation());
        assertEquals("com.example.Category", statement.getEntity());
        assertEquals("base", statement.getMetadata().get("batch"));
        assertEquals(10, statement.getMetadata().get("priority"));
        assertEquals(2, statement.getColumns().size());

        ImportColumn code = statement.getColumns().get(0);
        assertEquals("code", code.getName());
        assertEquals("feed", code.getMetadata().get("source"));
        assertEquals(Boolean.TRUE, code.getMetadata().get("enabled"));

        ImportColumn name = statement.getColumns().get(1);
        assertEquals("Unnamed", name.getDefaultValue());

        assertEquals("&catFruit", statement.getRows().get(0).getKey());
        assertEquals("Categories", statement.getRows().get(0).getMetadata().get("sheet"));
        assertEquals(8, statement.getRows().get(0).getMetadata().get("line"));
        assertEquals("fruit", statement.getRows().get(0).getValues().get("code"));
    }

    private void assertProductUniqueUpsert(ImportStatement statement) {
        assertEquals(ImportOperation.UPSERT, statement.getOperation());
        assertEquals("com.example.Product", statement.getEntity());
        assertNull(statement.getQuery());
        assertTrue(statement.getLookup().isEmpty());
        assertEquals(6, statement.getColumns().size());

        assertTrue(statement.getColumns().get(0).isUnique());
        assertEquals("sku", statement.getColumns().get(0).getName());
        assertEquals("variant", statement.getColumns().get(1).getName());
        assertTrue(statement.getColumns().get(1).isUnique());
        assertTrue(statement.getColumns().get(1).isNullSearch());
        assertEquals("Unnamed", statement.getColumns().get(2).getDefaultValue());
        assertEquals("code", statement.getColumns().get(3).getReference());
        assertEquals("replace", statement.getColumns().get(4).getMode());
        assertEquals("true", statement.getColumns().get(5).getDefaultValue());

        assertNull(statement.getRows().get(0).getValues().get("variant"));
        assertEquals("red\\, green,featured", statement.getRows().get(0).getValues().get("tags"));
        assertEquals("P|2", statement.getRows().get(1).getValues().get("sku"));
        assertEquals("Milk Product", statement.getRows().get(1).getValues().get("name"));
    }

    private void assertCompoundUniqueReference(ImportStatement statement) {
        assertEquals(ImportOperation.UPSERT, statement.getOperation());
        assertEquals("com.example.Price", statement.getEntity());
        assertEquals("sku", statement.getColumns().get(0).getReference());
        assertTrue(statement.getColumns().get(0).isUnique());
        assertTrue(statement.getColumns().get(1).isUnique());
        assertEquals("AUD", statement.getRows().get(0).getValues().get("currency"));
    }

    private void assertLookupShorthand(ImportStatement statement) {
        assertEquals(ImportOperation.MODIFY, statement.getOperation());
        assertEquals("com.example.Product", statement.getEntity());
        assertEquals(1, statement.getLookup().size());
        ImportLookup lookup = statement.getLookup().get(0);
        assertEquals("sku", lookup.getField());
        assertEquals("sku", lookup.getColumn());
        assertEquals("STARTSWITH", lookup.getComparator());
        assertFalse(lookup.isNullSearch());
        assertEquals("prefix", lookup.getMetadata().get("reason"));
        assertEquals(1, lookup.getMetadata().get("pass"));
    }

    private void assertIndexedLookupWithSourceColumn(ImportStatement statement) {
        assertEquals(ImportOperation.MODIFY, statement.getOperation());
        assertEquals("com.example.Customer", statement.getEntity());
        assertEquals(2, statement.getLookup().size());

        ImportLookup emailHash = statement.getLookup().get(0);
        assertEquals("emailHash", emailHash.getField());
        assertEquals("email", emailHash.getColumn());
        assertEquals("EQUALS", emailHash.getComparator());
        assertEquals(Boolean.TRUE, emailHash.getMetadata().get("derived"));

        ImportLookup status = statement.getLookup().get(1);
        assertEquals("status", status.getField());
        assertNull(status.getColumn());
        assertEquals("EQUALS", status.getComparator());

        assertEquals("email", statement.getColumns().get(0).getName());
        assertEquals("emailHashHandler", statement.getColumns().get(0).getHandler());
        assertEquals("lower", statement.getColumns().get(0).getMetadata().get("case"));
        assertEquals("john@example.com", statement.getRows().get(0).getValues().get("email"));
    }

    private void assertColumnLevelLookup(ImportStatement statement) {
        assertEquals(1, statement.getLookup().size());
        ImportLookup lookup = statement.getLookup().get(0);
        assertEquals("emailHash", lookup.getField());
        assertNull(lookup.getColumn());
        assertEquals("EQUALS", lookup.getComparator());
        assertEquals(Boolean.TRUE, lookup.getMetadata().get("derived"));
        assertEquals("emailHashHandler", statement.getColumns().get(0).getHandler());
    }

    private void assertQueryParams(ImportStatement statement) {
        assertEquals(ImportOperation.MODIFY, statement.getOperation());
        ImportQuery query = statement.getQuery();
        assertNotNull(query);
        assertEquals("lower(sku) = :sku and status = :status", query.getText());
        assertEquals("jpa", query.getMetadata().get("backend"));
        assertNull(query.getParams().get("sku").getColumn());
        assertEquals(Boolean.TRUE, query.getParams().get("sku").getMetadata().get("normalized"));
        assertEquals("state", query.getParams().get("status").getColumn());
        assertEquals("lowercaseHandler", statement.getColumns().get(0).getHandler());
    }

    private void assertExplicitQueryParam(ImportStatement statement) {
        assertEquals(ImportOperation.MODIFY, statement.getOperation());
        assertEquals("sku = :sku", statement.getQuery().getText());
        assertEquals("sku", statement.getQuery().getParams().get("sku").getColumn());
        assertEquals("explicit", statement.getQuery().getParams().get("sku").getMetadata().get("source"));
    }

    private void assertDeleteLookup(ImportStatement statement) {
        assertEquals(ImportOperation.DELETE, statement.getOperation());
        assertEquals(1, statement.getLookup().size());
        assertEquals("sku", statement.getLookup().get(0).getField());
        assertNull(statement.getLookup().get(0).getColumn());
        assertEquals("EQUALS", statement.getLookup().get(0).getComparator());
    }

    private void assertFetchUniqueWithRowKey(ImportStatement statement) {
        assertEquals(ImportOperation.FETCH, statement.getOperation());
        assertTrue(statement.getColumns().get(0).isUnique());
        assertEquals("&existingUser", statement.getRows().get(0).getKey());
        assertEquals("support@example.com", statement.getRows().get(0).getValues().get("email"));
    }

    private void assertCollectionModes(ImportStatement statement) {
        assertEquals(ImportOperation.MODIFY, statement.getOperation());
        assertTrue(statement.getColumns().get(0).isUnique());
        assertEquals("code", statement.getColumns().get(1).getReference());
        assertEquals("append", statement.getColumns().get(1).getMode());
        assertEquals("clear", statement.getColumns().get(2).getMode());
        assertEquals("admin,buyer", statement.getRows().get(0).getValues().get("roles"));
        assertNull(statement.getRows().get(0).getValues().get("preferences"));
    }

    private void assertRowKeyReference(ImportStatement statement) {
        assertEquals(ImportOperation.CREATE, statement.getOperation());
        assertEquals("*", statement.getColumns().get(1).getReference());
        assertEquals("&existingUser", statement.getRows().get(0).getValues().get("manager"));
    }

    private String readSample() throws Exception {
        URL resource = getClass().getClassLoader().getResource("samples/guide-all-combinations.import");
        assertNotNull(resource);
        try {
            return Files.readString(Path.of(resource.toURI()), StandardCharsets.UTF_8);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Unable to load guide sample import file", exception);
        }
    }
}
