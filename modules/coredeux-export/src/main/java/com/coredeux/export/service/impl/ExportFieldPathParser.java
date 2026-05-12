package com.coredeux.export.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.model.ExportField;

public class ExportFieldPathParser {

    public List<ExportFieldPath> parse(List<ExportField> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new CoredeuxExportException("Export field list must not be empty");
        }
        List<ExportFieldPath> paths = new ArrayList<>();
        for (ExportField field : fields) {
            if (field == null || field.getPath() == null || field.getPath().isBlank()) {
                throw new CoredeuxExportException("Export field path must not be blank");
            }
            String expression = field.getPath().trim();
            List<String> segments = splitEscaped(expression, ':').stream()
                    .map(String::trim)
                    .toList();
            if (segments.stream().anyMatch(String::isBlank)) {
                throw new CoredeuxExportException("Export field path contains a blank segment: " + expression);
            }
            paths.add(new ExportFieldPath(expression, segments, field));
        }
        return List.copyOf(paths);
    }

    private List<String> splitEscaped(String value, char separator) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (escaped) {
                current.append(character);
                escaped = false;
                continue;
            }
            if (character == '\\') {
                escaped = true;
                continue;
            }
            if (character == separator) {
                result.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(character);
        }
        if (escaped) {
            current.append('\\');
        }
        result.add(current.toString());
        return result;
    }
}
