package com.coredeux.impex.parser.excel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportStatement;
import com.coredeux.impex.parser.exception.CoredeuxImportParserException;

class CoredeuxExcelImportParserTest {

    private final CoredeuxExcelImportParser parser = new CoredeuxExcelImportParser();

    @Test
    void shouldParseWorkbookCellsAsImportCells() throws IOException {
        ImportRequest request = parser.parse(workbook(workbook -> {
            Sheet sheet = workbook.createSheet("Products");
            strings(sheet.createRow(0), "OPTIONS(passes=2,failFast=false)");
            strings(sheet.createRow(1), "&Product=com.example.Product");
            strings(sheet.createRow(2),
                    "UPSERT &Product(lookup.1.field=sku,lookup.1.comparator=EQUALS,query=\"lower(sku) = :sku\",query.params=sku)",
                    "sku(unique,handler=lowercaseHandler)", "name", "tags(mode=append)");
            strings(sheet.createRow(3), "&p1(meta.batch=base)", "P-1", "Demo", "red\\,blue");
        }));

        assertEquals(2, request.getOptions().getPasses());
        ImportStatement statement = request.getStatements().get(0);
        assertEquals("com.example.Product", statement.getEntity());
        assertEquals("sku", statement.getLookup().get(0).getField());
        assertEquals("lower(sku) = :sku", statement.getQuery().getText());
        assertNull(statement.getQuery().getParams().get("sku").getColumn());
        assertEquals("&p1", statement.getRows().get(0).getKey());
        assertEquals("base", statement.getRows().get(0).getMetadata().get("batch"));
        assertEquals("red\\,blue", statement.getRows().get(0).getValues().get("tags"));
    }

    @Test
    void shouldSupportLongHeadersAcrossWorkbookCells() throws IOException {
        ImportRequest request = parser.parse(workbook(workbook -> {
            Sheet sheet = workbook.createSheet("Articles");
            strings(sheet.createRow(0), "CREATE com.example.Article(metadata.source=xlsx)");
            strings(sheet.createRow(1), "", "code", "body", "title");
            strings(sheet.createRow(2), "", "A-1", "Line one\nLine two", "Demo");
        }));

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("xlsx", statement.getMetadata().get("source"));
        assertEquals("Line one\nLine two", statement.getRows().get(0).getValues().get("body"));
        assertEquals("Demo", statement.getRows().get(0).getValues().get("title"));
    }

    @Test
    void shouldPreservePipeCharactersWhitespaceAndCommentLookingTextInsideCells() throws IOException {
        ImportRequest request = parser.parse(workbook(workbook -> {
            Sheet sheet = workbook.createSheet("Escapes");
            strings(sheet.createRow(0), "CREATE com.example.Product", "sku", "name", "description");
            strings(sheet.createRow(1), "", "P|1", "  Padded Name  ", "# visible value");
        }));

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("P|1", statement.getRows().get(0).getValues().get("sku"));
        assertEquals("  Padded Name  ", statement.getRows().get(0).getValues().get("name"));
        assertEquals("# visible value", statement.getRows().get(0).getValues().get("description"));
    }

    @Test
    void shouldParseFirstSheetWhenSheetNameIsNotProvided() throws IOException {
        ImportRequest request = parser.parse(workbook(workbook -> {
            Sheet products = workbook.createSheet("Products");
            strings(products.createRow(0), "CREATE com.example.Product", "sku", "name");
            strings(products.createRow(1), "", "P-1", "Demo");
            Sheet categories = workbook.createSheet("Categories");
            strings(categories.createRow(0), "CREATE com.example.Category", "code", "name");
            strings(categories.createRow(1), "", "fruit", "Fruit");
        }));

        assertEquals("com.example.Product", request.getStatements().get(0).getEntity());
        assertEquals("P-1", request.getStatements().get(0).getRows().get(0).getValues().get("sku"));
    }

    @Test
    void shouldParseFirstSheetWhenSheetArgumentsAreNullOrBlank() throws IOException {
        InputStream nullArgsWorkbook = workbook(workbook -> {
            Sheet products = workbook.createSheet("Products");
            strings(products.createRow(0), "CREATE com.example.Product", "sku");
            strings(products.createRow(1), "", "P-1");
        });
        ImportRequest nullArgsRequest = parser.parse(nullArgsWorkbook, (String[]) null);

        InputStream blankNameWorkbook = workbook(workbook -> {
            Sheet products = workbook.createSheet("Products");
            strings(products.createRow(0), "CREATE com.example.Product", "sku");
            strings(products.createRow(1), "", "P-2");
        });
        ImportRequest blankNameRequest = parser.parse(blankNameWorkbook, " ");

        assertEquals("P-1", nullArgsRequest.getStatements().get(0).getRows().get(0).getValues().get("sku"));
        assertEquals("P-2", blankNameRequest.getStatements().get(0).getRows().get(0).getValues().get("sku"));
    }

    @Test
    void shouldParseNamedSheetWhenSheetNameIsProvided() throws IOException {
        ImportRequest request = parser.parse(workbook(workbook -> {
            Sheet products = workbook.createSheet("Products");
            strings(products.createRow(0), "CREATE com.example.Product", "sku", "name");
            strings(products.createRow(1), "", "P-1", "Demo");
            Sheet categories = workbook.createSheet("Categories");
            strings(categories.createRow(0), "CREATE com.example.Category", "code", "name");
            strings(categories.createRow(1), "", "fruit", "Fruit");
        }), "Categories");

        assertEquals("com.example.Category", request.getStatements().get(0).getEntity());
        assertEquals("fruit", request.getStatements().get(0).getRows().get(0).getValues().get("code"));
    }

    @Test
    void shouldIgnoreNullAndBlankWorkbookRows() throws IOException {
        ImportRequest request = parser.parse(workbook(workbook -> {
            Sheet sheet = workbook.createSheet("Sparse");
            strings(sheet.createRow(0), "CREATE com.example.Product", "sku");
            Row blankRow = sheet.createRow(2);
            blankRow.createCell(0, CellType.BLANK);
            blankRow.createCell(1, CellType.BLANK);
            strings(sheet.createRow(3), "", "P-1");
        }));

        assertEquals(1, request.getStatements().get(0).getRows().size());
        assertEquals("P-1", request.getStatements().get(0).getRows().get(0).getValues().get("sku"));
    }

    @Test
    void shouldRejectMissingSheetName() throws IOException {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse(workbook(workbook -> workbook.createSheet("Products")), "Missing"));

        assertTrue(exception.getMessage().contains("Excel import sheet not found: Missing"));
    }

    @Test
    void shouldRejectMoreThanOneOptionalArgument() throws IOException {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse(workbook(workbook -> workbook.createSheet("Products")), "Products", "extra"));

        assertTrue(exception.getMessage().contains("supports only one optional argument"));
    }

    @Test
    void shouldRejectWorkbookWithoutSheets() throws IOException {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse(workbook(workbook -> {
                })));

        assertTrue(exception.getMessage().contains("must contain at least one sheet"));
    }

    @Test
    void shouldTreatBlankWorkbookCellsAsBlankParserCells() throws IOException {
        ImportRequest request = parser.parse(workbook(workbook -> {
            Sheet sheet = workbook.createSheet("Blanks");
            strings(sheet.createRow(0), "CREATE com.example.Product", "sku", "empty", "blank");
            strings(sheet.createRow(1), "", "P-1", "\"\"", "");
        }));

        ImportStatement statement = request.getStatements().get(0);
        assertEquals("", statement.getRows().get(0).getValues().get("empty"));
        assertNull(statement.getRows().get(0).getValues().get("blank"));
    }

    @Test
    void shouldRejectNonTextCellsWithWorkbookCoordinates() throws IOException {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse(workbook(workbook -> {
                    Sheet sheet = workbook.createSheet("BadCells");
                    strings(sheet.createRow(0), "CREATE com.example.Product", "sku");
                    Row row = sheet.createRow(1);
                    row.createCell(0).setCellValue("");
                    row.createCell(1).setCellValue(42D);
                })));

        assertTrue(exception.getMessage().contains("sheet 'BadCells'"));
        assertTrue(exception.getMessage().contains("row 2 cell 2"));
        assertTrue(exception.getMessage().contains(CellType.NUMERIC.name()));
    }

    @Test
    void shouldRejectFormulaCells() throws IOException {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse(workbook(workbook -> {
                    Sheet sheet = workbook.createSheet("Formula");
                    strings(sheet.createRow(0), "CREATE com.example.Product", "sku");
                    Row row = sheet.createRow(1);
                    row.createCell(0).setCellValue("");
                    row.createCell(1).setCellFormula("\"P-1\"");
                })));

        assertTrue(exception.getMessage().contains(CellType.FORMULA.name()));
    }

    @Test
    void shouldRejectNullStream() {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse(null));

        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void shouldWrapWorkbookParseFailures() {
        CoredeuxImportParserException exception = assertThrows(CoredeuxImportParserException.class,
                () -> parser.parse(new ByteArrayInputStream("not an xlsx workbook".getBytes())));

        assertTrue(exception.getMessage().contains("Failed to parse Excel import workbook"));
        assertTrue(exception.getCause() instanceof IOException);
    }

    private InputStream workbook(WorkbookWriter writer) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writer.write(workbook);
            workbook.write(output);
            return new ByteArrayInputStream(output.toByteArray());
        }
    }

    private void strings(Row row, String... values) {
        for (int index = 0; index < values.length; index++) {
            Cell cell = row.createCell(index, CellType.STRING);
            cell.setCellValue(values[index]);
        }
    }

    private interface WorkbookWriter {

        void write(Workbook workbook) throws IOException;
    }
}
