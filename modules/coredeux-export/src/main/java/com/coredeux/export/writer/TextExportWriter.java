package com.coredeux.export.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportOptions;

@Component
public class TextExportWriter implements ExportWriter {

    @Override
    public ExportWriteSession open(Path targetFile, ExportOptions options) {
        try {
            BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8);
            String separator = options.getTextSeparator() == null || options.getTextSeparator().isEmpty()
                    ? "|"
                    : options.getTextSeparator();
            return new TextExportWriteSession(writer, separator, Boolean.TRUE.equals(options.getIncludeHeader()));
        } catch (IOException exception) {
            throw new CoredeuxExportException("Unable to open text export file: " + targetFile, exception);
        }
    }

    @Override
    public String contentType() {
        return "text/plain;charset=UTF-8";
    }

    @Override
    public String extension() {
        return ".txt";
    }

    private static final class TextExportWriteSession implements ExportWriteSession {

        private final BufferedWriter writer;
        private final String separator;
        private final boolean includeHeader;
        private boolean headerWritten;

        private TextExportWriteSession(BufferedWriter writer, String separator, boolean includeHeader) {
            this.writer = writer;
            this.separator = separator;
            this.includeHeader = includeHeader;
        }

        @Override
        public void writeHeader(List<String> values) throws IOException {
            if (includeHeader && !headerWritten) {
                writeLine(values);
                headerWritten = true;
            }
        }

        @Override
        public void writeRow(List<String> values) throws IOException {
            writeLine(values);
        }

        @Override
        public void finish() throws IOException {
            writer.flush();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        private void writeLine(List<String> values) throws IOException {
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    writer.write(separator);
                }
                writer.write(escape(values.get(index)));
            }
            writer.newLine();
        }

        private String escape(String value) {
            if (value == null) {
                return "";
            }
            boolean needsQuotes = value.contains(separator) || value.contains("\n") || value.contains("\r")
                    || value.contains("\"");
            if (!needsQuotes) {
                return value;
            }
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
    }
}
