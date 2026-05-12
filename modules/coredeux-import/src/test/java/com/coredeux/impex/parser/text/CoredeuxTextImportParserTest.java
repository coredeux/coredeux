package com.coredeux.impex.parser.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.model.ImportOperation;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportStatement;
import com.coredeux.impex.parser.exception.CoredeuxImportParserException;

class CoredeuxTextImportParserTest {

    private final CoredeuxTextImportParser parser = new CoredeuxTextImportParser();

    @Test
    void shouldParseAliasesStatementColumnsAndRows() {
        ImportRequest request = parser.parse("""
                &Product=com.example.Product

                UPSERT &Product | sku(unique=true) | name | parent(reference=sku) | active(default=true)
                                | SKU-1            | One  |                    | 
                &row2           | SKU-2            | Two  | SKU-1              | false
                """);

        assertEquals("com.example.Product", request.getMacros().get("&Product").getValue());
        assertEquals(1, request.getStatements().size());
        ImportStatement statement = request.getStatements().get(0);
        assertEquals(ImportOperation.UPSERT, statement.getOperation());
        assertEquals("com.example.Product", statement.getEntity());
        assertEquals(4, statement.getColumns().size());
        assertTrue(statement.getColumns().get(0).isUnique());
        assertEquals("sku", statement.getColumns().get(2).getReference());
        assertEquals("true", statement.getColumns().get(3).getDefaultValue());
        assertEquals(2, statement.getRows().size());
        assertNull(statement.getRows().get(0).getKey());
        assertEquals("SKU-1", statement.getRows().get(0).getValues().get("sku"));
        assertEquals("&row2", statement.getRows().get(1).getKey());
        assertEquals("SKU-1", statement.getRows().get(1).getValues().get("parent"));
    }

    @Test
    void shouldParseLookupAndQueryMetadata() {
        ImportRequest request = parser.parse("""
                MODIFY com.example.Product(query="sku = :sku") | sku(queryParam=sku) | name(handler=trimHandler)
                                                               | SKU-1               | Demo
                MODIFY com.example.Customer | emailHash(lookup.field=emailHash,lookup.comparator=EQUALS,handler=emailHashHandler) | name
                                            | john@example.com                                                          | John
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("sku = :sku", statement.getQuery().getText());
        assertEquals("sku", statement.getQuery().getParams().get("sku").getColumn());
        assertEquals("trimHandler", statement.getColumns().get(1).getHandler());
        ImportStatement lookupStatement = request.getStatements().get(1);
        assertEquals(1, lookupStatement.getLookup().size());
        assertEquals("emailHash", lookupStatement.getLookup().get(0).getField());
        assertNull(lookupStatement.getLookup().get(0).getColumn());
        assertEquals("emailHashHandler", lookupStatement.getColumns().get(0).getHandler());
    }

    @Test
    void shouldParseRequestOptionsAndMetadataAcrossModel() {
        ImportRequest request = parser.parse("""
                OPTIONS(passes=3,failFast=true,validateOnly=true)
                &Product=com.example.Product(source=legacy,priority=7)

                UPSERT &Product(metadata.batch=base,meta.active=true) | sku(unique=true,metadata.source=legacy,meta.priority=9) | name
                &row1(sheet=Products,line=12)                         | SKU-1                                                     | Demo
                """);

        assertEquals(3, request.getOptions().getPasses());
        assertTrue(request.getOptions().isFailFast());
        assertTrue(request.getOptions().isValidateOnly());
        assertEquals("legacy", request.getMacros().get("&Product").getMetadata().get("source"));
        assertEquals(7, request.getMacros().get("&Product").getMetadata().get("priority"));
        ImportStatement statement = request.getStatements().get(0);
        assertEquals("base", statement.getMetadata().get("batch"));
        assertEquals(true, statement.getMetadata().get("active"));
        assertEquals("legacy", statement.getColumns().get(0).getMetadata().get("source"));
        assertEquals(9, statement.getColumns().get(0).getMetadata().get("priority"));
        assertEquals("&row1", statement.getRows().get(0).getKey());
        assertEquals("Products", statement.getRows().get(0).getMetadata().get("sheet"));
        assertEquals(12, statement.getRows().get(0).getMetadata().get("line"));
    }

    @Test
    void shouldParseStatementLookupShorthandAndIndexedLookups() {
        ImportRequest request = parser.parse("""
                MODIFY com.example.Product(lookup=sku:sku:STARTSWITH:false,lookup.meta.reason=prefix,lookup.1.field=status,lookup.1.comparator=EQUALS,lookup.1.nullSearch=true)
                                          | sku | status | name
                                          | ABC | ACTIVE | Demo
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals(2, statement.getLookup().size());
        assertEquals("sku", statement.getLookup().get(0).getField());
        assertEquals("sku", statement.getLookup().get(0).getColumn());
        assertEquals("STARTSWITH", statement.getLookup().get(0).getComparator());
        assertFalse(statement.getLookup().get(0).isNullSearch());
        assertEquals("prefix", statement.getLookup().get(0).getMetadata().get("reason"));
        assertEquals("status", statement.getLookup().get(1).getField());
        assertNull(statement.getLookup().get(1).getColumn());
        assertEquals("EQUALS", statement.getLookup().get(1).getComparator());
        assertTrue(statement.getLookup().get(1).isNullSearch());
    }

    @Test
    void shouldParseStatementQueryParamsAndMetadataWithParentheses() {
        ImportRequest request = parser.parse("""
                MODIFY com.example.Product(query="lower(sku) = :sku and status = :status",query.params="sku,status:state",query.meta.backend=jpa,query.param.sku.meta.normalized=true)
                                          | sku(handler=lowercaseHandler) | state | name
                                          | SKU-1                         | LIVE  | Demo
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("lower(sku) = :sku and status = :status", statement.getQuery().getText());
        assertNull(statement.getQuery().getParams().get("sku").getColumn());
        assertEquals("state", statement.getQuery().getParams().get("status").getColumn());
        assertEquals("jpa", statement.getQuery().getMetadata().get("backend"));
        assertEquals(true, statement.getQuery().getParams().get("sku").getMetadata().get("normalized"));
    }

    @Test
    void shouldPreserveEscapedSeparatorsInCellsAndOptions() {
        ImportRequest request = parser.parse("""
                CREATE com.example.Product | sku | name(default="A\\, B") | description
                                           | S\\|1 | Demo\\|Product       | left\\|right
                """);

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("A, B", statement.getColumns().get(1).getDefaultValue());
        assertEquals("S|1", statement.getRows().get(0).getValues().get("sku"));
        assertEquals("Demo|Product", statement.getRows().get(0).getValues().get("name"));
        assertEquals("left|right", statement.getRows().get(0).getValues().get("description"));
    }

    @Test
    void shouldCarryUnknownHeaderMetadataOnColumn() {
        ImportRequest request = parser.parse("""
                CREATE com.example.Product | sku(source=legacy,format=upper)
                                           | SKU-1
                """);

        assertEquals("legacy", request.getStatements().get(0).getColumns().get(0).getMetadata().get("source"));
        assertEquals("upper", request.getStatements().get(0).getColumns().get(0).getMetadata().get("format"));
    }

    @Test
    void shouldRejectRowsBeforeStatements() {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse("| orphan"));

        assertTrue(exception.getMessage().contains("Line 1"));
    }

    @Test
    void shouldRejectWrongCellCount() {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse("""
                        CREATE com.example.Product | sku | name
                                                   | only-one
                        """));

        assertTrue(exception.getMessage().contains("Expected 2 values"));
    }

    @Test
    void shouldRejectDuplicateColumns() {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse("CREATE com.example.Product | sku | sku"));

        assertTrue(exception.getMessage().contains("Duplicate column name"));
    }

    @Test
    void shouldRejectStatementWithoutColumns() {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse("CREATE com.example.Product"));

        assertTrue(exception.getMessage().contains("at least one column"));
    }
}
