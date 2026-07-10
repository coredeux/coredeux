package com.coredeux.impex.parser.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.parser.CoredeuxImportParser;
import com.coredeux.impex.parser.exception.CoredeuxImportParserException;
import com.coredeux.impex.parser.text.CoredeuxTextImportParser;

public class CoredeuxExcelImportParser implements CoredeuxImportParser<InputStream> {

    private final CoredeuxTextImportParser textParser;

    /**
     * Creates an Excel parser that compiles workbook cells through the standard
     * pipe-text parser.
     */
    public CoredeuxExcelImportParser() {
        this(new CoredeuxTextImportParser());
    }

    /**
     * Allows tests or embedding applications to provide a configured text parser.
     */
    CoredeuxExcelImportParser(CoredeuxTextImportParser textParser) {
        this.textParser = textParser;
    }

    /**
     * Parses a named workbook sheet, or the first sheet when no name is provided,
     * into the canonical import request model.
     */
    @Override
    public ImportRequest parse(InputStream source, String... args) {
        if (source == null) {
            throw new CoredeuxImportParserException("Excel import stream must not be null");
        }
        String sheetName = sheetName(args);
        try (Workbook workbook = WorkbookFactory.create(source)) {
            return textParser.parse(toPipeText(resolveSheet(workbook, sheetName)));
        } catch (EncryptedDocumentException | IOException exception) {
            throw new CoredeuxImportParserException("Failed to parse Excel import workbook", exception);
        }
    }

    /**
     * Extracts the optional sheet-name argument from the shared parser contract.
     */
    private String sheetName(String... args) {
        if (args == null || args.length == 0) {
            return null;
        }
        if (args.length > 1) {
            throw new CoredeuxImportParserException("Excel import parser supports only one optional argument: sheet name");
        }
        return args[0];
    }

    /**
     * Resolves the target sheet from the optional sheet name.
     */
    private Sheet resolveSheet(Workbook workbook, String sheetName) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new CoredeuxImportParserException("Excel import workbook must contain at least one sheet");
        }
        if (sheetName == null || sheetName.isBlank()) {
            return workbook.getSheetAt(0);
        }
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new CoredeuxImportParserException("Excel import sheet not found: " + sheetName);
        }
        return sheet;
    }

    /**
     * Converts the selected workbook sheet into the text parser's pipe-separated
     * representation.
     */
    private String toPipeText(Sheet sheet) {
        StringBuilder text = new StringBuilder();
        appendSheet(text, sheet);
        return text.toString();
    }

    /**
     * Appends one sheet's rows while preserving workbook cell boundaries.
     */
    private void appendSheet(StringBuilder text, Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
            if (!text.isEmpty()) {
                text.append(System.lineSeparator());
            }
            text.append(toPipeLine(sheet, sheet.getRow(rowIndex)));
        }
    }

    /**
     * Converts one workbook row into one parser line.
     */
    private String toPipeLine(Sheet sheet, Row row) {
        if (row == null || row.getLastCellNum() < 0) {
            return "";
        }
        List<String> cells = new ArrayList<>();
        int lastCellIndex = lastNonBlankCellIndex(row);
        if (lastCellIndex < 0) {
            return "";
        }
        for (int cellIndex = 0; cellIndex <= lastCellIndex; cellIndex++) {
            cells.add(toPipeCell(sheet, row, cellIndex));
        }
        if (cells.stream().allMatch(String::isEmpty)) {
            return "";
        }
        return String.join(" | ", cells);
    }

    /**
     * Finds the last non-blank cell in a row so styled empty cells at the end do
     * not leak into the parser as trailing separators.
     */
    private int lastNonBlankCellIndex(Row row) {
        for (int cellIndex = row.getLastCellNum() - 1; cellIndex >= 0; cellIndex--) {
            Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return cellIndex;
            }
        }
        return -1;
    }

    /**
     * Reads a single Excel cell as text and escapes parser-level separators.
     */
    private String toPipeCell(Sheet sheet, Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() != CellType.STRING) {
            throw unsupportedCellType(sheet, row, cellIndex, cell.getCellType());
        }
        return escapeCell(cell.getStringCellValue());
    }

    /**
     * Quotes or escapes a cell value so the text parser receives the same value
     * that was present in the workbook cell.
     */
    private String escapeCell(String value) {
        String normalized = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        String escaped = normalized.replace("\\", "\\\\").replace("|", "\\|");
        boolean quote = escaped.contains("\n") || hasOuterWhitespace(escaped) || escaped.startsWith("#");
        if (!quote) {
            return escaped;
        }
        return "\"" + escaped.replace("\"", "\\\"") + "\"";
    }

    /**
     * Detects leading or trailing whitespace that the text parser would
     * otherwise trim from row values.
     */
    private boolean hasOuterWhitespace(String value) {
        return !value.isEmpty() && (!value.equals(value.trim()));
    }

    /**
     * Creates a parser exception with workbook coordinates for unsupported cell
     * types.
     */
    private CoredeuxImportParserException unsupportedCellType(Sheet sheet, Row row, int cellIndex, CellType cellType) {
        return new CoredeuxImportParserException("Excel sheet '" + sheet.getSheetName() + "' row "
                + (row.getRowNum() + 1) + " cell " + (cellIndex + 1)
                + " must be text or blank but was " + cellType);
    }
}
