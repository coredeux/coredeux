package com.coredeux.impex.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.model.ImportOperation;
import com.coredeux.impex.model.ImportStatement;

class ImportEntityMetadataTest {

    @Test
    void shouldExposeGeneratedValueAccessorsAndEquality() {
        ImportStatement statement = ImportStatement.builder()
                .operation(ImportOperation.CREATE)
                .entity(String.class.getName())
                .build();
        ImportEntityMetadata metadata = ImportEntityMetadata.builder()
                .targetClass(String.class)
                .identifierPath("id")
                .statement(statement)
                .build();
        ImportEntityMetadata same = ImportEntityMetadata.builder()
                .targetClass(String.class)
                .identifierPath("id")
                .statement(statement)
                .build();
        ImportEntityMetadata different = ImportEntityMetadata.builder()
                .targetClass(Integer.class)
                .identifierPath("id")
                .statement(statement)
                .build();

        assertSame(String.class, metadata.getTargetClass());
        assertEquals("id", metadata.getIdentifierPath());
        assertSame(statement, metadata.getStatement());
        assertEquals(same, metadata);
        assertEquals(same.hashCode(), metadata.hashCode());
        assertNotEquals(different, metadata);
        assertTrue(metadata.toString().contains("identifierPath=id"));
    }
}
