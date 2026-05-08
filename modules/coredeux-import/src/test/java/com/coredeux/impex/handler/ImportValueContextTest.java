package com.coredeux.impex.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.model.ImportMacro;
import com.coredeux.impex.model.ImportOperation;
import com.coredeux.impex.model.ImportRow;
import com.coredeux.impex.model.ImportStatement;

class ImportValueContextTest {

    @Test
    void shouldExposeGeneratedValueMethods() {
        ImportColumn column = ImportColumn.builder().name("sku").build();
        ImportRow row = ImportRow.builder().key("&row").values(new LinkedHashMap<>(Map.of("sku", "A-1"))).build();
        ImportStatement statement = ImportStatement.builder()
                .operation(ImportOperation.CREATE)
                .entity("Product")
                .build();
        Map<String, ImportMacro> macros = new LinkedHashMap<>(Map.of("&Product", ImportMacro.builder()
                .value("Product")
                .build()));
        Map<String, String> references = new LinkedHashMap<>(Map.of("&row", "1"));

        ImportValueContext context = ImportValueContext.builder()
                .rawValue(42)
                .effectiveValue("42")
                .expectedType(Integer.class)
                .collectionElementType(String.class)
                .mapValueType(Object.class)
                .targetEntityType(Product.class)
                .column(column)
                .row(row)
                .statement(statement)
                .macros(macros)
                .references(references)
                .build();
        ImportValueContext same = ImportValueContext.builder()
                .rawValue(42)
                .effectiveValue("42")
                .expectedType(Integer.class)
                .collectionElementType(String.class)
                .mapValueType(Object.class)
                .targetEntityType(Product.class)
                .column(column)
                .row(row)
                .statement(statement)
                .macros(macros)
                .references(references)
                .build();

        assertEquals(same, context);
        assertEquals(same.hashCode(), context.hashCode());
        assertNotEquals(context, ImportValueContext.builder().rawValue(43).build());
        assertTrue(context.toString().contains("effectiveValue=42"));
        assertEquals(42, context.getRawValue());
        assertEquals("42", context.getEffectiveValue());
        assertSame(Integer.class, context.getExpectedType());
        assertSame(String.class, context.getCollectionElementType());
        assertSame(Object.class, context.getMapValueType());
        assertSame(Product.class, context.getTargetEntityType());
        assertSame(column, context.getColumn());
        assertSame(row, context.getRow());
        assertSame(statement, context.getStatement());
        assertSame(macros, context.getMacros());
        assertSame(references, context.getReferences());
    }

    @Test
    void shouldDetectGeneratedEqualityDifferencesAcrossFields() {
        ImportValueContext base = ImportValueContext.builder()
                .rawValue(42)
                .effectiveValue("42")
                .expectedType(Integer.class)
                .collectionElementType(String.class)
                .mapValueType(Object.class)
                .targetEntityType(Product.class)
                .column(ImportColumn.builder().name("sku").build())
                .row(ImportRow.builder().key("&row").build())
                .statement(ImportStatement.builder().operation(ImportOperation.CREATE).entity("Product").build())
                .macros(new LinkedHashMap<>(Map.of("&Product", ImportMacro.builder().value("Product").build())))
                .references(new LinkedHashMap<>(Map.of("&row", "1")))
                .build();

        assertNotEquals(base, null);
        assertNotEquals(base, "not a context");
        assertNotEquals(base, baseVariation().rawValue(43).build());
        assertNotEquals(base, baseVariation().effectiveValue("43").build());
        assertNotEquals(base, baseVariation().expectedType(Long.class).build());
        assertNotEquals(base, baseVariation().collectionElementType(Integer.class).build());
        assertNotEquals(base, baseVariation().mapValueType(String.class).build());
        assertNotEquals(base, baseVariation().targetEntityType(Customer.class).build());
        assertNotEquals(base, baseVariation().column(ImportColumn.builder().name("name").build()).build());
        assertNotEquals(base, baseVariation().row(ImportRow.builder().key("&other").build()).build());
        assertNotEquals(base, baseVariation().statement(ImportStatement.builder()
                .operation(ImportOperation.MODIFY)
                .entity("Product")
                .build()).build());
        assertNotEquals(base, baseVariation().macros(new LinkedHashMap<>()).build());
        assertNotEquals(base, baseVariation().references(new LinkedHashMap<>()).build());
    }

    private ImportValueContext.ImportValueContextBuilder baseVariation() {
        return ImportValueContext.builder()
                .rawValue(42)
                .effectiveValue("42")
                .expectedType(Integer.class)
                .collectionElementType(String.class)
                .mapValueType(Object.class)
                .targetEntityType(Product.class)
                .column(ImportColumn.builder().name("sku").build())
                .row(ImportRow.builder().key("&row").build())
                .statement(ImportStatement.builder().operation(ImportOperation.CREATE).entity("Product").build())
                .macros(new LinkedHashMap<>(Map.of("&Product", ImportMacro.builder().value("Product").build())))
                .references(new LinkedHashMap<>(Map.of("&row", "1")));
    }

    private static class Product {
    }

    private static class Customer {
    }
}
