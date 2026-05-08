package com.coredeux.impex.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.registry.InMemoryEntityDefinitionRegistry;
import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.model.ImportLookup;
import com.coredeux.impex.model.ImportOperation;
import com.coredeux.impex.model.ImportQuery;
import com.coredeux.impex.model.ImportQueryParam;
import com.coredeux.impex.model.ImportRow;
import com.coredeux.impex.model.ImportStatement;

class ImportEntityTargetServiceTest {

    private ImportEntityTargetService targetService;

    @BeforeEach
    void setUp() {
        targetService = new ImportEntityTargetService(new DefaultCoredeuxReflectionHelperService(),
                new InMemoryEntityDefinitionRegistry(List.of(CoredeuxEntityDefinition.builder()
                        .fullClassName(Target.class.getName())
                        .name("Target")
                        .identifier("code")
                        .build())));
    }

    @Test
    void shouldResolveTargetWithDefinitionIdentifierAndNullRows() {
        ImportStatement statement = ImportStatement.builder()
                .operation(ImportOperation.CREATE)
                .entity(Target.class.getName())
                .columns(List.of(ImportColumn.builder().name("code").build()))
                .rows(null)
                .build();

        ImportEntityMetadata metadata = targetService.resolveTarget(statement);

        assertEquals(Target.class, metadata.getTargetClass());
        assertEquals("code", metadata.getIdentifierPath());
        assertEquals(statement, metadata.getStatement());
    }

    @Test
    void shouldRejectInvalidStatementShapes() {
        assertImportError("entity must not be blank", () -> targetService.resolveTarget(null));
        assertImportError("entity must not be blank", () -> targetService.resolveTarget(ImportStatement.builder()
                .operation(ImportOperation.CREATE)
                .entity(" ")
                .columns(List.of(ImportColumn.builder().name("code").build()))
                .build()));
        assertImportError("Import column must not be null", () -> targetService.resolveTarget(statement(null)));
        assertImportError("Import row key must not be blank", () -> targetService.resolveTarget(ImportStatement.builder()
                .operation(ImportOperation.CREATE)
                .entity(Target.class.getName())
                .columns(List.of(ImportColumn.builder().name("code").build()))
                .rows(List.of(ImportRow.builder().key(" ").build()))
                .build()));
    }

    @Test
    void shouldRejectInvalidLookupAndQueryDeclarations() {
        assertImportError("Import lookup must not be null", () -> targetService.resolveTarget(ImportStatement.builder()
                .operation(ImportOperation.MODIFY)
                .entity(Target.class.getName())
                .columns(List.of(ImportColumn.builder().name("code").build()))
                .lookup(nullLookupList())
                .build()));
        assertImportError("Import lookup field must not be blank", () -> targetService.resolveTarget(ImportStatement.builder()
                .operation(ImportOperation.MODIFY)
                .entity(Target.class.getName())
                .columns(List.of(ImportColumn.builder().name("code").build()))
                .lookup(List.of(ImportLookup.builder().field(" ").build()))
                .build()));
        assertImportError("does not match any statement column", () -> targetService.resolveTarget(ImportStatement.builder()
                .operation(ImportOperation.MODIFY)
                .entity(Target.class.getName())
                .columns(List.of(ImportColumn.builder().name("code").build()))
                .lookup(List.of(ImportLookup.builder().field("active").build()))
                .build()));
        assertImportError("Import query text must not be blank", () -> targetService.resolveTarget(ImportStatement.builder()
                .operation(ImportOperation.MODIFY)
                .entity(Target.class.getName())
                .columns(List.of(ImportColumn.builder().name("code").build()))
                .query(ImportQuery.builder().text(" ").params(Map.of("code", ImportQueryParam.builder().build())).build())
                .build()));
        assertImportError("must define at least one parameter", () -> targetService.resolveTarget(ImportStatement.builder()
                .operation(ImportOperation.MODIFY)
                .entity(Target.class.getName())
                .columns(List.of(ImportColumn.builder().name("code").build()))
                .query(ImportQuery.builder().text("code = :code").build())
                .build()));
        assertImportError("parameter name must not be blank", () -> targetService.resolveTarget(ImportStatement.builder()
                .operation(ImportOperation.MODIFY)
                .entity(Target.class.getName())
                .columns(List.of(ImportColumn.builder().name("code").build()))
                .query(ImportQuery.builder().text("code = :code").params(Map.of(" ", ImportQueryParam.builder().build()))
                        .build())
                .build()));
    }

    @Test
    void shouldCreateInstancesAndReportConstructorFailures() {
        Object target = targetService.createInstance(ImportEntityMetadata.builder().targetClass(Target.class).build());

        assertInstanceOf(Target.class, target);
        assertImportError("Unable to create instance", () -> targetService.createInstance(ImportEntityMetadata.builder()
                .targetClass(NoDefaultConstructor.class)
                .build()));
    }

    @Test
    void shouldWriteScalarAndCollectionValuesWithModes() {
        Target target = new Target();
        ImportEntityMetadata metadata = ImportEntityMetadata.builder().targetClass(Target.class).identifierPath("code").build();

        targetService.writeValue(target, ImportColumn.builder().name("code").build(), "SKU-1", metadata);
        targetService.writeValue(target, ImportColumn.builder().name("active").build(), true, metadata);
        targetService.writeValue(target, ImportColumn.builder().name("tags").mode("replace").build(),
                new LinkedHashSet<>(List.of("red")), metadata);
        targetService.writeValue(target, ImportColumn.builder().name("tags").mode("append").build(),
                new LinkedHashSet<>(List.of("blue")), metadata);
        targetService.writeValue(target, ImportColumn.builder().name("notes").mode("append").build(),
                List.of("note-1"), metadata);
        targetService.writeValue(target, ImportColumn.builder().name("tags").mode("clear").build(), null, metadata);

        assertEquals("SKU-1", target.code);
        assertTrue(target.active);
        assertTrue(target.tags.isEmpty());
        assertEquals(List.of("note-1"), target.notes);
        assertEquals("SKU-1", targetService.identifier(target, metadata));
    }

    @Test
    void shouldRejectInvalidWrites() {
        Target target = new Target();
        ImportEntityMetadata metadata = ImportEntityMetadata.builder().targetClass(Target.class).identifierPath("code").build();

        assertImportError("Unable to resolve field",
                () -> targetService.writeValue(target, ImportColumn.builder().name("missing").build(), "x", metadata));
        assertImportError("expects boolean",
                () -> targetService.writeValue(target, ImportColumn.builder().name("active").build(), null, metadata));
        assertImportError("expects java.util.Set",
                () -> targetService.writeValue(target, ImportColumn.builder().name("tags").build(), "red", metadata));
        assertImportError("expects java.util.Set", () -> targetService.writeValue(target,
                ImportColumn.builder().name("tags").mode("replace").build(), new ArrayList<>(List.of("red")), metadata));
        assertImportError("Unsupported collection mode", () -> targetService.writeValue(target,
                ImportColumn.builder().name("tags").mode("merge").build(), new LinkedHashSet<>(), metadata));
    }

    private ImportStatement statement(ImportColumn column) {
        return ImportStatement.builder()
                .operation(ImportOperation.CREATE)
                .entity(Target.class.getName())
                .columns(nullColumnList(column))
                .build();
    }

    private List<ImportColumn> nullColumnList(ImportColumn column) {
        List<ImportColumn> columns = new ArrayList<>();
        columns.add(column);
        return columns;
    }

    private List<ImportLookup> nullLookupList() {
        List<ImportLookup> lookups = new ArrayList<>();
        lookups.add(null);
        return lookups;
    }

    private void assertImportError(String expectedMessage, Runnable action) {
        CoredeuxImportException exception = assertThrows(CoredeuxImportException.class, action::run);

        assertTrue(exception.getMessage().contains(expectedMessage),
                () -> "Expected message to contain '" + expectedMessage + "' but was: " + exception.getMessage());
    }

    private static class Target {

        private String code;
        private boolean active;
        private Set<String> tags;
        private List<String> notes;
    }

    private static class NoDefaultConstructor {

        private NoDefaultConstructor(String ignored) {
        }
    }
}
