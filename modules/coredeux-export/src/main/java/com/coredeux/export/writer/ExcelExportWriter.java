package com.coredeux.export.writer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportOptions;

public class ExcelExportWriter implements ExportWriter {

    @Override
    public ExportWriteSession open(Path targetFile, ExportOptions options) {
        try {
            return new ExcelExportWriteSession(targetFile, Boolean.TRUE.equals(options.getIncludeHeader()));
        } catch (IOException exception) {
            throw new CoredeuxExportException("Unable to open XLSX export file: " + targetFile, exception);
        }
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String extension() {
        return ".xlsx";
    }

    private static final class ExcelExportWriteSession implements ExportWriteSession {

        private final Path targetFile;
        private final SXSSFWorkbook workbook;
        private final Sheet sheet;
        private final boolean includeHeader;
        private int rowIndex;

        private ExcelExportWriteSession(Path targetFile, boolean includeHeader) throws IOException {
            this.targetFile = targetFile;
            this.includeHeader = includeHeader;
            this.workbook = new SXSSFWorkbook(100);
            this.sheet = workbook.createSheet("export");
        }

        @Override
        public void writeHeader(List<String> values) {
            if (includeHeader) {
                writeRow(values);
            }
        }

        @Override
        public void writeRow(List<String> values) {
            Row row = sheet.createRow(rowIndex++);
            for (int index = 0; index < values.size(); index++) {
                Cell cell = row.createCell(index);
                cell.setCellValue(values.get(index) == null ? "" : values.get(index));
            }
        }

        @Override
        public void finish() throws IOException {
            try (var output = Files.newOutputStream(targetFile)) {
                workbook.write(output);
            } finally {
                workbook.dispose();
                workbook.close();
            }
        }

        @Override
        public void close() throws IOException {
            workbook.dispose();
            workbook.close();
        }
    }
}
