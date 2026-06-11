package com.coredeux.impex.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.handler.service.impl.DefaultCoredeuxValueHandlerService;
import com.coredeux.core.helper.impl.DefaultCoredeuxReflectionHelperService;
import com.coredeux.core.registry.InMemoryEntityDefinitionRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.impex.handler.CoredeuxImportValueHandler;
import com.coredeux.impex.handler.impl.DefaultCoredeuxImportValueHandler;
import com.coredeux.impex.handler.impl.JsonMapImportHandler;
import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.model.ImportLookup;
import com.coredeux.impex.model.ImportOperation;
import com.coredeux.impex.model.ImportOptions;
import com.coredeux.impex.model.ImportQuery;
import com.coredeux.impex.model.ImportQueryParam;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportResponse;
import com.coredeux.impex.model.ImportRow;
import com.coredeux.impex.model.ImportStatement;
import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultCoredeuxImportServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RecordingCoredeuxService coredeuxService;
    private DefaultCoredeuxImportService importService;

    @BeforeEach
    void setUp() {
        coredeuxService = new RecordingCoredeuxService();
        DefaultCoredeuxReflectionHelperService reflection = new DefaultCoredeuxReflectionHelperService();
        InMemoryEntityDefinitionRegistry registry = new InMemoryEntityDefinitionRegistry(List.of(
                CoredeuxEntityDefinition.builder()
                        .fullClassName(Product.class.getName())
                        .name("Product")
                        .identifier("id")
                        .build(),
                CoredeuxEntityDefinition.builder()
                        .fullClassName(ProductOwner.class.getName())
                        .name("ProductOwner")
                        .identifier("id")
                        .build()));
        ImportEntityTargetService entityTargetService = new ImportEntityTargetService(reflection, registry);
        InMemoryCoredeuxComponentRegistry componentRegistry = InMemoryCoredeuxComponentRegistry.builder()
                .component("coredeuxDefaultImportValueHandler", new DefaultCoredeuxImportValueHandler(coredeuxService))
                .component("uppercaseHandler", (CoredeuxImportValueHandler) context -> context.getEffectiveValue().toUpperCase())
                .component("wrongTypeHandler", (CoredeuxImportValueHandler) context -> "not-a-number")
                .component("jsonMapImportHandler", new JsonMapImportHandler())
                .build();
        importService = new DefaultCoredeuxImportService(coredeuxService, reflection, entityTargetService,
                new DefaultCoredeuxValueHandlerService(componentRegistry));
    }

    @Test
    void shouldValidatePojoColumnsAgainstEntityFields() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(ImportColumn.builder().name("missing").build()))
                        .build()))
                .build();

        ImportResponse response = importService.validateData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("missing field"));
    }

    @Test
    void shouldRejectBlankColumnNamesDuringValidation() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(ImportColumn.builder().name(" ").build()))
                        .build()))
                .build();

        ImportResponse response = importService.validateData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("column name must not be blank"));
    }

    @Test
    void shouldRejectDuplicateColumnNamesDuringValidation() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("sku").build()))
                        .build()))
                .build();

        ImportResponse response = importService.validateData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("Duplicate import column name: sku"));
    }

    @Test
    void shouldRejectBlankRowKeysDuringValidation() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(ImportColumn.builder().name("sku").build()))
                        .rows(List.of(row(" ", Map.of("sku", "sku-blank-row-key"))))
                        .build()))
                .build();

        ImportResponse response = importService.validateData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("row key must not be blank"));
    }

    @Test
    void shouldRejectInvalidCollectionModesDuringValidation() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(ImportColumn.builder().name("tags").mode("merge").build()))
                        .build()))
                .build();

        ImportResponse response = importService.validateData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("Unsupported collection mode"));
    }

    @Test
    void shouldRejectCollectionModesOnScalarColumnsDuringValidation() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(ImportColumn.builder().name("price").mode("append").build()))
                        .build()))
                .build();

        ImportResponse response = importService.validateData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("can only be used for collection column"));
    }

    @Test
    void shouldCreatePojoUsingDefaultTypeConversions() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(productStatement(ImportOperation.CREATE,
                        row("&product1", Map.of(
                                "sku", "sku-1",
                                "price", "123456789",
                                "active", "true",
                                "category", "HARDWARE",
                                "tags", "featured, sale")))))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        Product saved = coredeuxService.saved(Product.class).get(0);
        assertEquals("1", saved.id);
        assertEquals("sku-1", saved.sku);
        assertEquals(new BigInteger("123456789"), saved.price);
        assertTrue(saved.active);
        assertEquals(ProductCategory.HARDWARE, saved.category);
        assertEquals(new LinkedHashSet<>(List.of("featured", "sale")), saved.tags);
    }

    @Test
    void shouldPreserveEscapedCommasInPrimitiveCollectionValues() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(productStatement(ImportOperation.CREATE,
                        row("&product-escaped-tags", Map.of(
                                "sku", "sku-escaped-tags",
                                "price", "10",
                                "active", "true",
                                "category", "SOFTWARE",
                                "tags", "\\,leading,middle\\,comma,trailing\\,,endslash\\, slash\\\\,next")))))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        Product saved = coredeuxService.saved(Product.class).get(0);
        assertEquals(new LinkedHashSet<>(List.of(",leading", "middle,comma", "trailing,", "endslash, slash\\",
                "next")), saved.tags);
    }

    @Test
    void shouldRejectMapValuesWhenDefaultHandlerIsUsed() {
        ImportRequest request = ImportRequest.builder()
                .options(ImportOptions.builder().passes(1).build())
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("attributes").build()))
                        .rows(List.of(row("&product-map-string", Map.of(
                                "sku", "sku-map-string",
                                "attributes", Map.of("color", "blue", "size", 42)))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertTrue(response.hasErrors());
        assertEquals("attributes", response.getLogs().get(0).getColumn());
        assertTrue(response.getLogs().get(0).getMessage().contains("No default import conversion"));
    }

    @Test
    void shouldImportStringMapValuesWithJsonMapHandler() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("attributes").handler("jsonMapImportHandler").build()))
                        .rows(List.of(row("&product-map-string", Map.of(
                                "sku", "sku-map-string",
                                "attributes", Map.of("color", "blue", "size", 42)))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        Product saved = coredeuxService.saved(Product.class).get(0);
        assertEquals(Map.of("color", "blue", "size", "42"), saved.attributes);
    }

    @Test
    void shouldImportStringMapValuesFromJsonStringWithJsonMapHandler() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("attributes").handler("jsonMapImportHandler").build()))
                        .rows(List.of(row("&product-map-string-json", Map.of(
                                "sku", "sku-map-string-json",
                                "attributes", "{\"color\":\"blue\",\"size\":42}"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        Product saved = coredeuxService.saved(Product.class).get(0);
        assertEquals(Map.of("color", "blue", "size", "42"), saved.attributes);
    }

    @Test
    void shouldImportObjectMapPrimitiveValuesWithJsonMapHandler() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("family", "demo");
        metadata.put("priority", 7);
        metadata.put("active", true);
        metadata.put("ratio", 1.5d);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("metadata").handler("jsonMapImportHandler").build()))
                        .rows(List.of(row("&product-map-object", Map.of(
                                "sku", "sku-map-object",
                                "metadata", metadata))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        Product saved = coredeuxService.saved(Product.class).get(0);
        assertEquals(metadata, saved.metadata);
    }

    @Test
    void shouldConvertMapValuesToDeclaredGenericTypeWithJsonMapHandler() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("ratings").handler("jsonMapImportHandler").build()))
                        .rows(List.of(row("&product-map-integer", Map.of(
                                "sku", "sku-map-integer",
                                "ratings", Map.of("quality", "10", "support", 8)))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        Product saved = coredeuxService.saved(Product.class).get(0);
        assertEquals(Map.of("quality", 10, "support", 8), saved.ratings);
    }

    @Test
    void shouldImportParsedMapAndJsonStringForSameColumnAcrossRows() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("attributes").handler("jsonMapImportHandler").build()))
                        .rows(List.of(
                                row("&product-map-parsed", Map.of(
                                        "sku", "sku-map-parsed",
                                        "attributes", Map.of("source", "parser", "rank", 1))),
                                row("&product-map-json", Map.of(
                                        "sku", "sku-map-json",
                                        "attributes", "{\"source\":\"json\",\"rank\":2}"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        List<Product> saved = coredeuxService.saved(Product.class);
        assertEquals(Map.of("source", "parser", "rank", "1"), saved.get(0).attributes);
        assertEquals(Map.of("source", "json", "rank", "2"), saved.get(1).attributes);
    }

    @Test
    void shouldReturnNullForBlankJsonMapString() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("attributes").handler("jsonMapImportHandler").build()))
                        .rows(List.of(row("&product-map-blank", Map.of(
                                "sku", "sku-map-blank",
                                "attributes", "   "))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertNull(coredeuxService.saved(Product.class).get(0).attributes);
    }

    @Test
    void shouldRejectMalformedJsonMapStringWithColumnContext() {
        ImportResponse response = importService.importData(mapImportRequest("attributes", "{bad-json"));

        assertTrue(response.hasErrors());
        assertEquals("attributes", response.getLogs().get(0).getColumn());
        assertTrue(response.getLogs().get(0).getMessage().contains("Failed to parse JSON map"));
    }

    @Test
    void shouldRejectNonObjectJsonMapStringWithColumnContext() {
        ImportResponse arrayResponse = importService.importData(mapImportRequest("attributes", "[\"a\"]"));
        ImportResponse primitiveResponse = importService.importData(mapImportRequest("attributes", "\"value\""));

        assertTrue(arrayResponse.hasErrors());
        assertEquals("attributes", arrayResponse.getLogs().get(0).getColumn());
        assertTrue(arrayResponse.getLogs().get(0).getMessage().contains("must be an object"));
        assertTrue(primitiveResponse.hasErrors());
        assertEquals("attributes", primitiveResponse.getLogs().get(0).getColumn());
        assertTrue(primitiveResponse.getLogs().get(0).getMessage().contains("must be an object"));
    }

    @Test
    void shouldRejectNestedMapValuesWithJsonMapHandler() {
        ImportRequest request = ImportRequest.builder()
                .options(ImportOptions.builder().passes(1).build())
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("metadata").handler("jsonMapImportHandler").build()))
                        .rows(List.of(row("&product-map-nested", Map.of(
                                "sku", "sku-map-nested",
                                "metadata", Map.of("nested", Map.of("name", "demo"))))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertTrue(response.hasErrors());
        assertEquals("metadata", response.getLogs().get(0).getColumn());
        assertTrue(response.getLogs().get(0).getMessage().contains("primitive, boxed, and string values"));
    }

    @Test
    void shouldUpsertExistingPojoByUniqueColumns() {
        Product existing = new Product();
        existing.id = "existing-id";
        existing.sku = "sku-2";
        existing.price = BigInteger.ONE;
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(productStatement(ImportOperation.UPSERT,
                        row("&product2", Map.of(
                                "sku", "sku-2",
                                "price", "99",
                                "active", "true",
                                "category", "SERVICE",
                                "tags", "support")))))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertEquals(new BigInteger("99"), existing.price);
        assertEquals(ProductCategory.SERVICE, existing.category);
    }

    @Test
    void shouldReplaceExistingCollectionByDefault() {
        Product existing = product("existing-replace-id", "sku-replace");
        existing.tags = new LinkedHashSet<>(List.of("old", "legacy"));
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").unique(true).build(),
                                ImportColumn.builder().name("tags").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-replace", "tags", "new, fresh"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertEquals(new LinkedHashSet<>(List.of("new", "fresh")), existing.tags);
    }

    @Test
    void shouldAppendToExistingCollectionWhenColumnModeIsAppend() {
        Product existing = product("existing-append-id", "sku-append");
        existing.tags = new LinkedHashSet<>(List.of("old"));
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").unique(true).build(),
                                ImportColumn.builder().name("tags").mode("append").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-append", "tags", "new, fresh"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertEquals(new LinkedHashSet<>(List.of("old", "new", "fresh")), existing.tags);
    }

    @Test
    void shouldClearExistingCollectionWhenColumnModeIsClear() {
        Product existing = product("existing-clear-id", "sku-clear");
        existing.tags = new LinkedHashSet<>(List.of("old", "legacy"));
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").unique(true).build(),
                                ImportColumn.builder().name("tags").mode("clear").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-clear"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertTrue(existing.tags.isEmpty());
    }

    @Test
    void shouldInitializeEmptyCollectionWhenColumnModeIsClearAndFieldIsNull() {
        Product existing = product("existing-clear-null-id", "sku-clear-null");
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").unique(true).build(),
                                ImportColumn.builder().name("tags").mode("clear").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-clear-null"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertEquals(new LinkedHashSet<>(), existing.tags);
    }

    @Test
    void shouldUseIsNullComparatorForNullUniqueLookupWhenNullSearchIsTrue() {
        Product existing = product("existing-null-id", null);
        existing.price = BigInteger.ONE;
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").unique(true).nullSearch(true).build(),
                                ImportColumn.builder().name("price").build()))
                        .rows(List.of(row(null, Map.of("price", "42"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertEquals(new BigInteger("42"), existing.price);
        assertEquals("ISNULL", coredeuxService.lastLoadAllParams.get(0).getComparator());
    }

    @Test
    void shouldModifyExistingPojoUsingStatementLookupWithoutUniqueColumns() {
        Product existing = product("existing-lookup-id", "sku-lookup-existing");
        existing.price = BigInteger.ONE;
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .lookup(List.of(ImportLookup.builder()
                                .field("sku")
                                .comparator("STARTSWITH")
                                .build()))
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("price").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-lookup", "price", "42"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertEquals(new BigInteger("42"), existing.price);
        assertEquals("sku", coredeuxService.lastLoadAllParams.get(0).getField());
        assertEquals("STARTSWITH", coredeuxService.lastLoadAllParams.get(0).getComparator());
        assertEquals("sku-lookup", coredeuxService.lastLoadAllParams.get(0).getValue());
    }

    @Test
    void shouldUseDerivedValueFromLookupColumnWhenColumnIsExplicit() {
        Product existing = product("existing-derived-lookup-id", "old-sku");
        existing.normalizedSku = "SKU-LOOKUP";
        existing.price = BigInteger.ONE;
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .lookup(List.of(ImportLookup.builder()
                                .field("normalizedSku")
                                .column("sku")
                                .build()))
                        .columns(List.of(
                                ImportColumn.builder().name("sku").handler("uppercaseHandler").build(),
                                ImportColumn.builder().name("price").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-lookup", "price", "99"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertEquals(new BigInteger("99"), existing.price);
        assertEquals("normalizedSku", coredeuxService.lastLoadAllParams.get(0).getField());
        assertEquals("SKU-LOOKUP", coredeuxService.lastLoadAllParams.get(0).getValue());
    }

    @Test
    void shouldRequireUniqueColumnOrLookupForNonCreateOperations() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("price").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-missing-lookup", "price", "1"))))
                        .build()))
                .build();

        ImportResponse response = importService.validateData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("unique columns, lookup, or query"));
    }

    @Test
    void shouldRejectLookupThatMatchesMultipleRows() {
        Product first = product("first-lookup-id", "sku-duplicate-1");
        Product second = product("second-lookup-id", "sku-duplicate-2");
        coredeuxService.put(first.id, first);
        coredeuxService.put(second.id, second);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .lookup(List.of(ImportLookup.builder()
                                .field("sku")
                                .comparator("STARTSWITH")
                                .build()))
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("price").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-duplicate", "price", "42"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("More than one entity found for lookup"));
    }

    @Test
    void shouldModifyExistingPojoUsingStatementQueryWithoutUniqueColumns() {
        Product existing = product("existing-query-id", "sku-query");
        existing.price = BigInteger.ONE;
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .query(ImportQuery.builder()
                                .text("sku = {{sku}}")
                                .params(Map.of("sku", ImportQueryParam.builder().build()))
                                .build())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("price").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-query", "price", "42"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertEquals(new BigInteger("42"), existing.price);
        assertEquals("sku = {{sku}}", coredeuxService.lastQueryText);
        assertEquals(Map.of("sku", "sku-query"), coredeuxService.lastQueryParams);
    }

    @Test
    void shouldUseDerivedQueryParamFromExplicitColumn() {
        Product existing = product("existing-query-derived-id", "old-sku");
        existing.normalizedSku = "SKU-QUERY";
        existing.price = BigInteger.ONE;
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .query(ImportQuery.builder()
                                .text("normalizedSku = {{normalizedSku}}")
                                .params(Map.of("normalizedSku", ImportQueryParam.builder().column("sku").build()))
                                .build())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").handler("uppercaseHandler").build(),
                                ImportColumn.builder().name("price").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-query", "price", "99"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertSame(existing, coredeuxService.updated.get(0));
        assertEquals(new BigInteger("99"), existing.price);
        assertEquals(Map.of("normalizedSku", "SKU-QUERY"), coredeuxService.lastQueryParams);
    }

    @Test
    void shouldRejectStatementWithMoreThanOneResolutionStrategy() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .query(ImportQuery.builder()
                                .text("sku = {{sku}}")
                                .params(Map.of("sku", ImportQueryParam.builder().build()))
                                .build())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").unique(true).build(),
                                ImportColumn.builder().name("price").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-conflict", "price", "1"))))
                        .build()))
                .build();

        ImportResponse response = importService.validateData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("Only one existing-entity resolution strategy"));
    }

    @Test
    void shouldRejectQueryThatMatchesMultipleRows() {
        Product first = product("first-query-id", "first");
        first.normalizedSku = "DUPLICATE";
        Product second = product("second-query-id", "second");
        second.normalizedSku = "DUPLICATE";
        coredeuxService.put(first.id, first);
        coredeuxService.put(second.id, second);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .query(ImportQuery.builder()
                                .text("normalizedSku = {{normalizedSku}}")
                                .params(Map.of("normalizedSku", ImportQueryParam.builder().column("sku").build()))
                                .build())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").handler("uppercaseHandler").build(),
                                ImportColumn.builder().name("price").build()))
                        .rows(List.of(row(null, Map.of("sku", "duplicate", "price", "42"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("More than one entity found for query"));
    }

    @Test
    void shouldRejectUnsupportedCollectionMode() {
        Product existing = product("existing-mode-id", "sku-mode");
        existing.tags = new LinkedHashSet<>(List.of("old"));
        coredeuxService.put(existing.id, existing);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.MODIFY)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").unique(true).build(),
                                ImportColumn.builder().name("tags").mode("merge").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-mode", "tags", "new"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("Unsupported collection mode"));
        assertEquals("tags", response.getLogs().get(0).getColumn());
    }

    @Test
    void shouldUseCustomColumnHandlerWhenProvided() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").handler("uppercaseHandler").build(),
                                ImportColumn.builder().name("price").defaultValue("10").build(),
                                ImportColumn.builder().name("active").defaultValue("false").build(),
                                ImportColumn.builder().name("category").defaultValue("SOFTWARE").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-custom"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertEquals("SKU-CUSTOM", coredeuxService.saved(Product.class).get(0).sku);
    }

    @Test
    void shouldReportHandlerOutputThatCannotBeAssignedToTargetField() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name("price").handler("wrongTypeHandler").build()))
                        .rows(List.of(row(null, Map.of("sku", "sku-bad", "price", "1"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertTrue(response.hasErrors());
        assertTrue(response.getLogs().get(0).getMessage().contains("expects java.math.BigInteger"));
        assertEquals("price", response.getLogs().get(0).getColumn());
    }

    @Test
    void shouldResolveReferencesUsingColumnReferenceMetadata() {
        Product product = new Product();
        product.id = "product-id";
        product.sku = "sku-ref";
        coredeuxService.put(product.id, product);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(ProductOwner.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("email").build(),
                                ImportColumn.builder().name("product").reference("sku").build()))
                        .rows(List.of(row(null, Map.of("email", "owner@example.com", "product", "sku-ref"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        ProductOwner owner = coredeuxService.saved(ProductOwner.class).get(0);
        assertInstanceOf(Product.class, owner.product);
        assertSame(product, owner.product);
    }

    @Test
    void shouldResolveCollectionReferencesUsingColumnReferenceMetadata() {
        Product first = product("product-1", "sku-list-1");
        Product second = product("product-2", "sku-list-2");
        coredeuxService.put(first.id, first);
        coredeuxService.put(second.id, second);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(ProductOwner.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("email").build(),
                                ImportColumn.builder().name("products").reference("sku").build()))
                        .rows(List.of(row(null, Map.of("email", "owner-list@example.com",
                                "products", "sku-list-1, sku-list-2"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        ProductOwner owner = coredeuxService.saved(ProductOwner.class).get(0);
        assertEquals(new LinkedHashSet<>(List.of(first, second)), owner.products);
    }

    @Test
    void shouldPreserveEscapedCommasInCollectionReferenceValues() {
        Product first = product("product-escaped-1", "sku,escaped");
        Product second = product("product-escaped-2", "sku-normal");
        coredeuxService.put(first.id, first);
        coredeuxService.put(second.id, second);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(ProductOwner.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("email").build(),
                                ImportColumn.builder().name("products").reference("sku").build()))
                        .rows(List.of(row(null, Map.of("email", "owner-escaped-list@example.com",
                                "products", "sku\\,escaped, sku-normal"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        ProductOwner owner = coredeuxService.saved(ProductOwner.class).get(0);
        assertEquals(new LinkedHashSet<>(List.of(first, second)), owner.products);
    }

    @Test
    void shouldHonorValidateOnlyOptionWithoutSavingRows() {
        ImportRequest request = ImportRequest.builder()
                .options(ImportOptions.builder().validateOnly(true).build())
                .statements(List.of(productStatement(ImportOperation.CREATE,
                        row("&product-validate-only", Map.of(
                                "sku", "sku-validate-only",
                                "price", "1",
                                "active", "true",
                                "category", "SOFTWARE",
                                "tags", "dry-run")))))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertTrue(coredeuxService.saved(Product.class).isEmpty());
    }

    @Test
    void shouldImportDistinctRowsWithIdenticalValues() {
        Map<String, Object> values = Map.of(
                "sku", "same-sku",
                "price", "1",
                "active", "true",
                "category", "SOFTWARE",
                "tags", "same");
        ImportStatement statement = productStatement(ImportOperation.CREATE, row(null, values));
        statement.setRows(List.of(row(null, values), row(null, values)));
        ImportRequest request = ImportRequest.builder().statements(List.of(statement)).build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertEquals(2, coredeuxService.saved(Product.class).size());
    }

    @Test
    void shouldResolveReferencesUsingPriorRowKeyMetadata() {
        ImportRequest request = ImportRequest.builder()
                .statements(List.of(
                        productStatement(ImportOperation.CREATE,
                                row("&product-key", Map.of(
                                        "sku", "sku-key-ref",
                                        "price", "12",
                                        "active", "true",
                                        "category", "HARDWARE",
                                        "tags", "keyed"))),
                        ImportStatement.builder()
                                .operation(ImportOperation.CREATE)
                                .entity(ProductOwner.class.getName())
                                .columns(List.of(
                                        ImportColumn.builder().name("email").build(),
                                        ImportColumn.builder().name("product").reference("*").build()))
                                .rows(List.of(row(null, Map.of("email", "owner-key@example.com",
                                        "product", "&product-key"))))
                                .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        Product product = coredeuxService.saved(Product.class).get(0);
        ProductOwner owner = coredeuxService.saved(ProductOwner.class).get(0);
        assertSame(product, owner.product);
    }

    @Test
    void shouldResolveCompoundReferencesUsingColumnReferenceMetadata() {
        Product product = product("product-compound", "sku-compound");
        product.category = ProductCategory.HARDWARE;
        coredeuxService.put(product.id, product);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(ProductOwner.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("email").build(),
                                ImportColumn.builder().name("product").reference("sku:category").build()))
                        .rows(List.of(row(null, Map.of("email", "owner-compound@example.com",
                                "product", "sku-compound:HARDWARE"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        ProductOwner owner = coredeuxService.saved(ProductOwner.class).get(0);
        assertSame(product, owner.product);
    }

    @Test
    void shouldPreserveEscapedColonsInCompoundReferenceValues() {
        Product product = product("product-compound-escaped", "sku:compound");
        product.category = ProductCategory.HARDWARE;
        coredeuxService.put(product.id, product);

        ImportRequest request = ImportRequest.builder()
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(ProductOwner.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("email").build(),
                                ImportColumn.builder().name("product").reference("sku:category").build()))
                        .rows(List.of(row(null, Map.of("email", "owner-compound-escaped@example.com",
                                "product", "sku\\:compound:HARDWARE"))))
                        .build()))
                .build();

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        ProductOwner owner = coredeuxService.saved(ProductOwner.class).get(0);
        assertSame(product, owner.product);
    }

    @Test
    void shouldCollectMultipleFinalPassErrorsWhenFailFastIsFalse() {
        ImportStatement statement = ImportStatement.builder()
                .operation(ImportOperation.CREATE)
                .entity(Product.class.getName())
                .columns(List.of(
                        ImportColumn.builder().name("sku").build(),
                        ImportColumn.builder().name("price").build()))
                .rows(List.of(
                        row(null, Map.of("sku", "bad-1", "price", "not-a-number")),
                        row(null, Map.of("sku", "bad-2", "price", "also-not-a-number"))))
                .build();
        ImportRequest request = ImportRequest.builder()
                .options(ImportOptions.builder().passes(1).failFast(false).build())
                .statements(List.of(statement))
                .build();

        ImportResponse response = importService.importData(request);

        assertTrue(response.hasErrors());
        assertEquals(2, response.getLogs().size());
        assertEquals(1, response.getLogs().get(0).getStatementIndex());
        assertEquals(1, response.getLogs().get(0).getRowIndex());
        assertEquals("price", response.getLogs().get(0).getColumn());
        assertEquals(1, response.getLogs().get(1).getStatementIndex());
        assertEquals(2, response.getLogs().get(1).getRowIndex());
        assertEquals("price", response.getLogs().get(1).getColumn());
    }

    @Test
    void shouldStopAtFirstFinalPassErrorWhenFailFastIsTrue() {
        ImportStatement statement = ImportStatement.builder()
                .operation(ImportOperation.CREATE)
                .entity(Product.class.getName())
                .columns(List.of(
                        ImportColumn.builder().name("sku").build(),
                        ImportColumn.builder().name("price").build()))
                .rows(List.of(
                        row(null, Map.of("sku", "bad-1", "price", "not-a-number")),
                        row(null, Map.of("sku", "bad-2", "price", "also-not-a-number"))))
                .build();
        ImportRequest request = ImportRequest.builder()
                .options(ImportOptions.builder().passes(1).failFast(true).build())
                .statements(List.of(statement))
                .build();

        ImportResponse response = importService.importData(request);

        assertTrue(response.hasErrors());
        assertEquals(1, response.getLogs().size());
        assertEquals("price", response.getLogs().get(0).getColumn());
    }

    @Test
    void shouldExecuteRawJsonGuideHappyPathSample() throws Exception {
        ImportRequest request = rawJsonSample("raw-json-happy-path.json");

        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        Product product = coredeuxService.saved(Product.class).get(0);
        assertEquals("raw-json-sku", product.sku);
        assertEquals(new BigInteger("10"), product.price);
        assertTrue(product.active);
        assertEquals(ProductCategory.SOFTWARE, product.category);
        assertEquals(new LinkedHashSet<>(List.of("primary, literal", "featured")), product.tags);
        assertEquals(Map.of("color", "blue", "size", "42"), product.attributes);
        assertEquals(Map.of("createdBy", "guide", "score", 7, "published", true), product.metadata);
        ProductOwner owner = coredeuxService.saved(ProductOwner.class).get(0);
        assertSame(product, owner.product);
        assertEquals("raw-json-guide", request.getStatements().get(0).getMetadata().get("source"));
        assertEquals("resource", request.getStatements().get(0).getRows().get(0).getMetadata().get("source"));
    }

    @Test
    void shouldExecuteRawJsonGuideLookupAndQuerySample() throws Exception {
        Product lookupTarget = product("raw-lookup-id", "old-lookup");
        lookupTarget.normalizedSku = "RAW-QUERY";
        lookupTarget.price = BigInteger.ONE;
        Product queryTarget = product("raw-query-id", "old-query");
        queryTarget.normalizedSku = "RAW-QUERY-2";
        queryTarget.price = BigInteger.TWO;
        coredeuxService.put(lookupTarget.id, lookupTarget);
        coredeuxService.put(queryTarget.id, queryTarget);

        ImportRequest request = rawJsonSample("raw-json-lookup-query.json");
        ImportResponse response = importService.importData(request);

        assertFalse(response.hasErrors());
        assertEquals(new BigInteger("25"), lookupTarget.price);
        assertEquals(new BigInteger("35"), queryTarget.price);
        assertEquals("normalizedSku = :normalizedSku", coredeuxService.lastQueryText);
        assertEquals(Map.of("normalizedSku", "RAW-QUERY-2"), coredeuxService.lastQueryParams);
        assertEquals(Boolean.TRUE, request.getStatements().get(0).getLookup().get(0).getMetadata().get("derived"));
        assertEquals("upper", request.getStatements().get(0).getColumns().get(0).getMetadata().get("normalization"));
        assertEquals("test", request.getStatements().get(1).getQuery().getMetadata().get("backend"));
    }

    @Test
    void shouldValidateRawJsonGuideFailureSample() throws Exception {
        ImportRequest request = rawJsonSample("raw-json-validation-failures.json");

        ImportResponse response = importService.validateData(request);

        assertTrue(response.hasErrors());
        assertEquals(2, response.getLogs().size());
        assertTrue(response.getLogs().get(0).getMessage().contains("Only one existing-entity resolution strategy"));
        assertTrue(response.getLogs().get(1).getMessage().contains("unique columns, lookup, or query"));
    }

    private ImportStatement productStatement(ImportOperation operation, ImportRow row) {
        return ImportStatement.builder()
                .operation(operation)
                .entity(Product.class.getName())
                .columns(List.of(
                        ImportColumn.builder().name("sku").unique(!ImportOperation.CREATE.equals(operation)).build(),
                        ImportColumn.builder().name("price").build(),
                        ImportColumn.builder().name("active").build(),
                        ImportColumn.builder().name("category").build(),
                        ImportColumn.builder().name("tags").build()))
                .rows(List.of(row))
                .build();
    }

    private ImportRequest mapImportRequest(String columnName, Object value) {
        return ImportRequest.builder()
                .options(ImportOptions.builder().passes(1).build())
                .statements(List.of(ImportStatement.builder()
                        .operation(ImportOperation.CREATE)
                        .entity(Product.class.getName())
                        .columns(List.of(
                                ImportColumn.builder().name("sku").build(),
                                ImportColumn.builder().name(columnName).handler("jsonMapImportHandler").build()))
                        .rows(List.of(row("&product-map-error", Map.of(
                                "sku", "sku-map-error",
                                columnName, value))))
                        .build()))
                .build();
    }

    private ImportRow row(String key, Map<String, Object> values) {
        return ImportRow.builder().key(key).values(new LinkedHashMap<>(values)).build();
    }

    private ImportRequest rawJsonSample(String fileName) throws Exception {
        URL resource = getClass().getClassLoader().getResource("samples/" + fileName);
        assertTrue(resource != null, "Missing raw JSON sample: " + fileName);
        String json = Files.readString(Path.of(resource.toURI()), StandardCharsets.UTF_8);
        return OBJECT_MAPPER.readValue(json, ImportRequest.class);
    }

    private Product product(String id, String sku) {
        Product product = new Product();
        product.id = id;
        product.sku = sku;
        return product;
    }

    private enum ProductCategory {
        SOFTWARE,
        HARDWARE,
        SERVICE
    }

    public static class Product {

        private String id;
        private String sku;
        private BigInteger price;
        private boolean active;
        private ProductCategory category;
        private String normalizedSku;
        private Set<String> tags;
        private Map<String, String> attributes;
        private Map<String, Integer> ratings;
        private Map<String, Object> metadata;
    }

    public static class ProductOwner {

        private String id;
        private String email;
        private Product product;
        private Set<Product> products;
    }

    private static final class RecordingCoredeuxService implements CoredeuxService {

        private final Map<Class<?>, List<Object>> saved = new LinkedHashMap<>();
        private final Map<String, Object> byId = new LinkedHashMap<>();
        private final List<Object> updated = new ArrayList<>();
        private final List<Object> removed = new ArrayList<>();
        private List<SearchParams> lastLoadAllParams = List.of();
        private String lastQueryText;
        private Map<String, Object> lastQueryParams = Map.of();
        private int sequence;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T load(String id, Class<T> type) {
            return (T) byId.get(id);
        }

        @Override
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            lastQueryText = query;
            lastQueryParams = params;
            List<T> matches = byId.values().stream()
                    .filter(type::isInstance)
                    .map(value -> (T) value)
                    .filter(value -> matchesQuery(params, value))
                    .toList();
            return SearchResult.<T>builder().results(matches).build();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> SearchResult<T> loadAll(List<SearchParams> params, Class<T> type, int pageSize, int currentPage) {
            lastLoadAllParams = params;
            List<T> matches = byId.values().stream()
                    .filter(type::isInstance)
                    .map(value -> (T) value)
                    .filter(value -> matches(params, value))
                    .toList();
            return SearchResult.<T>builder().results(matches).build();
        }

        @Override
        public <T> String save(T entity) {
            String id = String.valueOf(++sequence);
            setField(entity, "id", id);
            byId.put(id, entity);
            saved.computeIfAbsent(entity.getClass(), ignored -> new ArrayList<>()).add(entity);
            return id;
        }

        @Override
        public <T> void update(T entity) {
            updated.add(entity);
        }

        @Override
        public <T> void remove(String id, Class<T> type) {
            removed.add(byId.remove(id));
        }

        @Override
        public <T> void remove(T entity) {
            removed.add(entity);
        }

        @Override
        public <T> void refresh(T entity) {
        }

        void put(String id, Object entity) {
            byId.put(id, entity);
        }

        @SuppressWarnings("unchecked")
        <T> List<T> saved(Class<T> type) {
            return (List<T>) (List<?>) saved.getOrDefault(type, List.of());
        }

        private boolean matches(List<SearchParams> params, Object value) {
            for (SearchParams param : params) {
                Object fieldValue = fieldValue(value, param.getField());
                if ("ISNULL".equals(param.getComparator())) {
                    if (fieldValue != null) {
                        return false;
                    }
                } else if ("STARTSWITH".equals(param.getComparator())) {
                    if (fieldValue == null || param.getValue() == null
                            || !String.valueOf(fieldValue).startsWith(String.valueOf(param.getValue()))) {
                        return false;
                    }
                } else if (!java.util.Objects.equals(fieldValue, param.getValue())) {
                    return false;
                }
            }
            return true;
        }

        private boolean matchesQuery(Map<String, Object> params, Object value) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (!java.util.Objects.equals(fieldValue(value, param.getKey()), param.getValue())) {
                    return false;
                }
            }
            return true;
        }

        private Object fieldValue(Object value, String fieldName) {
            try {
                Field field = value.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(value);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }

        private void setField(Object value, String fieldName, Object fieldValue) {
            try {
                Field field = value.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(value, fieldValue);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
