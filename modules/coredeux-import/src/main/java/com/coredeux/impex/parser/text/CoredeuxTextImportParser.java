package com.coredeux.impex.parser.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.coredeux.impex.model.ImportColumn;
import com.coredeux.impex.model.ImportLookup;
import com.coredeux.impex.model.ImportMacro;
import com.coredeux.impex.model.ImportOperation;
import com.coredeux.impex.model.ImportOptions;
import com.coredeux.impex.model.ImportQuery;
import com.coredeux.impex.model.ImportQueryParam;
import com.coredeux.impex.model.ImportRequest;
import com.coredeux.impex.model.ImportRow;
import com.coredeux.impex.model.ImportStatement;
import com.coredeux.impex.parser.CoredeuxImportParser;
import com.coredeux.impex.parser.exception.CoredeuxImportParserException;

public class CoredeuxTextImportParser implements CoredeuxImportParser<String> {

    private static final Pattern OPTIONS_PATTERN = Pattern.compile("^(OPTIONS|IMPORT)\\s*\\((.*)\\)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Parses Coredeux pipe-separated import text into the canonical import request
     * model.
     */
    @Override
    public ImportRequest parse(String source, String... args) {
        if (source == null) {
            throw new CoredeuxImportParserException("Import text must not be null");
        }
        if (args != null && args.length > 0) {
            throw new CoredeuxImportParserException("Text import parser does not support optional parser arguments");
        }
        ImportRequest request = ImportRequest.builder().build();
        ImportStatement current = null;
        for (SourceLine sourceLine : logicalLines(source)) {
            int index = sourceLine.lineIndex();
            String line = sourceLine.text();
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            java.util.regex.Matcher optionsMatcher = OPTIONS_PATTERN.matcher(trimmed);
            if (optionsMatcher.matches()) {
                parseRequestOptions(optionsMatcher.group(2), request, index);
                continue;
            }
            if (isMacroLine(trimmed)) {
                parseMacro(trimmed, request, index);
                continue;
            }
            if (startsWithOperation(trimmed)) {
                current = parseStatement(trimmed, request, index);
                request.getStatements().add(current);
                continue;
            }
            if (current == null) {
                throw error(index, "Row encountered before any import statement");
            }
            if (current.getColumns().isEmpty() && startsWithCellSeparator(line)) {
                addColumns(current, line.substring(separatorIndex(line, '|') + 1), index);
                validateStatementShape(current, index);
                continue;
            }
            validateStatementShape(current, index);
            parseRow(line, current, index);
        }
        for (int index = 0; index < request.getStatements().size(); index++) {
            validateStatementShape(request.getStatements().get(index), index);
        }
        return request;
    }

    /**
     * Converts physical file lines into logical parser lines by joining quoted
     * multiline values.
     */
    private List<SourceLine> logicalLines(String source) {
        String[] physicalLines = source.split("\\R", -1);
        List<SourceLine> logicalLines = new ArrayList<>();
        StringBuilder current = null;
        int startLine = 0;
        for (int index = 0; index < physicalLines.length; index++) {
            String line = index == 0 ? stripBom(physicalLines[index]) : physicalLines[index];
            if (current == null) {
                current = new StringBuilder(line);
                startLine = index;
            } else {
                current.append('\n').append(line);
            }
            if (quotedTextClosed(current)) {
                logicalLines.add(new SourceLine(current.toString(), startLine));
                current = null;
            }
        }
        if (current != null) {
            throw error(startLine, "Unterminated quoted value");
        }
        return logicalLines;
    }

    /**
     * Applies request-level options such as passes, fail-fast, and validate-only.
     */
    private void parseRequestOptions(String text, ImportRequest request, int lineIndex) {
        Map<String, String> options = parseOptions(text, lineIndex);
        ImportOptions importOptions = request.getOptions() == null ? ImportOptions.builder().build() : request.getOptions();
        if (options.containsKey("passes")) {
            importOptions.setPasses(intOption(options.get("passes"), "passes", lineIndex));
        }
        if (options.containsKey("failFast")) {
            importOptions.setFailFast(booleanValue(options.get("failFast")));
        }
        if (options.containsKey("validateOnly")) {
            importOptions.setValidateOnly(booleanValue(options.get("validateOnly")));
        }
        request.setOptions(importOptions);
    }

    /**
     * Parses a statement header into operation, entity, statement metadata,
     * lookup/query strategy, and optional inline column definitions.
     */
    private ImportStatement parseStatement(String line, ImportRequest request, int lineIndex) {
        int headerSeparator = separatorIndex(line, '|');
        String statementHeader = headerSeparator < 0 ? line : line.substring(0, headerSeparator).trim();
        String headerText = headerSeparator < 0 ? null : line.substring(headerSeparator + 1);
        int operationEnd = firstWhitespace(statementHeader);
        if (operationEnd < 0) {
            throw error(lineIndex, "Statement entity must not be blank");
        }
        ImportOperation operation = ImportOperation.valueOf(statementHeader.substring(0, operationEnd).toUpperCase(Locale.ROOT));
        String remainder = statementHeader.substring(operationEnd).trim();
        int entityEnd = entityEnd(remainder);
        String entity = resolveAlias(remainder.substring(0, entityEnd).trim(), request);
        String optionText = remainder.substring(entityEnd).trim();
        if (!optionText.isBlank()) {
            if (!optionText.startsWith("(")) {
                throw error(lineIndex, "Unexpected text after statement entity: " + optionText);
            }
            int close = matchingClose(optionText, 0);
            if (close != optionText.length() - 1) {
                throw error(lineIndex, "Invalid statement metadata: " + optionText);
            }
            optionText = optionText.substring(1, close);
        }
        Map<String, String> statementOptions = parseOptions(optionText, lineIndex);
        ImportStatement statement = ImportStatement.builder()
                .operation(operation)
                .entity(entity)
                .build();
        statementOptions.forEach((key, value) -> {
            if (!knownStatementOption(key)) {
                statement.getMetadata().put(metadataKey(key), metadataValue(value));
            }
        });
        String queryText = firstPresent(statementOptions, "query", "query.text");
        if (queryText != null && !queryText.isBlank()) {
            statement.setQuery(ImportQuery.builder().text(queryText).build());
        }
        addStatementLookups(statement, statementOptions, lineIndex);
        addStatementQueryParams(statement, statementOptions, lineIndex);
        applyQueryMetadata(statement, statementOptions);
        applyQueryParamMetadata(statement, statementOptions);
        if (headerText != null && !headerText.isBlank()) {
            addColumns(statement, headerText, lineIndex);
        }
        return statement;
    }

    /**
     * Parses all pipe-separated column header cells for a statement.
     */
    private void addColumns(ImportStatement statement, String headerText, int lineIndex) {
        for (String header : splitCells(headerText)) {
            if (!header.isBlank()) {
                addColumn(statement, header.trim(), lineIndex);
            }
        }
    }

    /**
     * Parses one column header and attaches column behavior, metadata, lookup, and
     * query-param declarations.
     */
    private void addColumn(ImportStatement statement, String header, int lineIndex) {
        HeaderSpec spec = parseHeader(header, lineIndex);
        if (spec.name().isBlank()) {
            throw error(lineIndex, "Column name must not be blank");
        }
        if (statement.getColumns().stream().anyMatch(column -> spec.name().equals(column.getName()))) {
            throw error(lineIndex, "Duplicate column name: " + spec.name());
        }
        ImportColumn column = ImportColumn.builder()
                .name(spec.name())
                .unique(booleanOption(spec.options(), "unique", false))
                .nullSearch(booleanOption(spec.options(), "nullSearch", false))
                .defaultValue(firstPresent(spec.options(), "default", "defaultValue"))
                .mode(spec.options().getOrDefault("mode", "replace"))
                .reference(spec.options().get("reference"))
                .handler(spec.options().get("handler"))
                .metadata(new LinkedHashMap<>())
                .build();
        spec.options().forEach((key, value) -> {
            if (!knownColumnOption(key)) {
                column.getMetadata().put(metadataKey(key), metadataValue(value));
            }
        });
        statement.getColumns().add(column);
        addLookup(statement, column, spec.options());
        addQueryParam(statement, column, spec.options());
    }

    /**
     * Adds a column-level lookup predicate when the column declares lookup
     * options.
     */
    private void addLookup(ImportStatement statement, ImportColumn column, Map<String, String> options) {
        String field = firstPresent(options, "lookup.field", "lookupField");
        boolean lookup = booleanOption(options, "lookup", false) || field != null;
        if (!lookup) {
            return;
        }
        ImportLookup importLookup = ImportLookup.builder()
                .field(field == null || field.isBlank() ? column.getName() : field)
                .column(firstPresent(options, "lookup.column", "lookupColumn"))
                .comparator(options.getOrDefault("lookup.comparator", options.getOrDefault("comparator", "EQUALS")))
                .nullSearch(booleanOption(options, "lookup.nullSearch", false))
                .metadata(new LinkedHashMap<>())
                .build();
        options.forEach((key, value) -> {
            if (key.startsWith("lookup.metadata.") || key.startsWith("lookup.meta.")) {
                importLookup.getMetadata().put(metadataKey(removeAnyPrefix(key, "lookup.metadata.", "lookup.meta.")),
                        metadataValue(value));
            }
        });
        statement.getLookup().add(importLookup);
    }

    /**
     * Adds a query parameter declaration sourced from the current column.
     */
    private void addQueryParam(ImportStatement statement, ImportColumn column, Map<String, String> options) {
        String paramName = firstPresent(options, "queryParam", "query.param", "queryParam.name", "query.param.name");
        if (paramName == null || paramName.isBlank()) {
            return;
        }
        if (statement.getQuery() == null) {
            statement.setQuery(ImportQuery.builder().build());
        }
        ImportQueryParam param = ImportQueryParam.builder().column(column.getName()).metadata(new LinkedHashMap<>()).build();
        options.forEach((key, value) -> {
            if (key.startsWith("queryParam.metadata.") || key.startsWith("queryParam.meta.")
                    || key.startsWith("query.param.metadata.") || key.startsWith("query.param.meta.")) {
                param.getMetadata().put(metadataKey(removeAnyPrefix(key, "queryParam.metadata.", "queryParam.meta.",
                        "query.param.metadata.", "query.param.meta.")), metadataValue(value));
            }
        });
        statement.getQuery().getParams().put(paramName, param);
    }

    /**
     * Parses one row into row key metadata and a column-name to raw-value map.
     */
    private void parseRow(String line, ImportStatement statement, int lineIndex) {
        List<String> cells = splitCells(line);
        if (cells.isEmpty()) {
            return;
        }
        int columnCount = statement.getColumns().size();
        if (columnCount == 0) {
            throw error(lineIndex, "Statement has no columns");
        }
        String key = null;
        Map<String, Object> rowMetadata = new LinkedHashMap<>();
        int valueOffset = 0;
        boolean explicitRowKeyCell = startsWithCellSeparator(line);
        if (cells.size() == columnCount + 1) {
            RowKeySpec rowKeySpec = parseRowKey(cells.get(0).trim(), lineIndex);
            key = emptyToNull(rowKeySpec.key());
            rowMetadata.putAll(rowKeySpec.metadata());
            valueOffset = 1;
        } else if (explicitRowKeyCell || cells.size() != columnCount) {
            throw error(lineIndex, "Expected " + columnCount + " values or " + (columnCount + 1)
                    + " cells with a row key but found " + cells.size());
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < columnCount; i++) {
            String cell = cells.get(i + valueOffset).trim();
            String value = unquote(cell);
            values.put(statement.getColumns().get(i).getName(), isQuoted(cell) ? value : emptyToNull(value));
        }
        statement.getRows().add(ImportRow.builder().key(key).values(values).metadata(rowMetadata).build());
    }

    /**
     * Splits a header cell into a column/entity name and its parenthesized
     * options.
     */
    private HeaderSpec parseHeader(String header, int lineIndex) {
        int open = header.indexOf('(');
        if (open < 0) {
            return new HeaderSpec(header.trim(), Map.of());
        }
        int close = matchingClose(header, open);
        if (close < open) {
            throw error(lineIndex, "Invalid header metadata: " + header);
        }
        if (!header.substring(close + 1).trim().isEmpty()) {
            throw error(lineIndex, "Unexpected text after header metadata: " + header);
        }
        String name = header.substring(0, open).trim();
        Map<String, String> options = parseOptions(header.substring(open + 1, close), lineIndex);
        return new HeaderSpec(name, options);
    }

    /**
     * Parses a row-key cell, including optional metadata attached to the key.
     */
    private RowKeySpec parseRowKey(String cell, int lineIndex) {
        if (cell.isBlank()) {
            return new RowKeySpec(null, Map.of());
        }
        int open = cell.indexOf('(');
        if (open < 0) {
            return new RowKeySpec(cell, Map.of());
        }
        int close = matchingClose(cell, open);
        if (close < open || !cell.substring(close + 1).trim().isEmpty()) {
            throw error(lineIndex, "Invalid row key metadata: " + cell);
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        parseOptions(cell.substring(open + 1, close), lineIndex)
                .forEach((key, value) -> metadata.put(metadataKey(key), metadataValue(value)));
        return new RowKeySpec(cell.substring(0, open).trim(), metadata);
    }

    /**
     * Parses comma-separated option assignments while respecting quotes and
     * escaped separators.
     */
    private Map<String, String> parseOptions(String text, int lineIndex) {
        Map<String, String> options = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return options;
        }
        for (String option : splitOptions(text)) {
            if (option.isBlank()) {
                continue;
            }
            int separator = option.indexOf('=');
            if (separator < 0) {
                options.put(option.trim(), "true");
            } else {
                String key = option.substring(0, separator).trim();
                if (key.isBlank()) {
                    throw error(lineIndex, "Option key must not be blank");
                }
                options.put(key, unquote(option.substring(separator + 1).trim()));
            }
        }
        return options;
    }

    /**
     * Adds statement-level lookup predicates from shorthand or indexed lookup
     * options.
     */
    private void addStatementLookups(ImportStatement statement, Map<String, String> options, int lineIndex) {
        if (containsAny(options, "lookup", "lookup.field", "lookupField")) {
            statement.getLookup().add(lookupFromOptions(options, "lookup", lineIndex));
        }
        for (String index : lookupIndexes(options)) {
            statement.getLookup().add(lookupFromOptions(options, "lookup." + index, lineIndex));
        }
    }

    /**
     * Builds one lookup predicate from a statement option prefix.
     */
    private ImportLookup lookupFromOptions(Map<String, String> options, String prefix, int lineIndex) {
        String shorthand = options.get(prefix);
        Map<String, String> shorthandValues = parseLookupShorthand(shorthand, lineIndex);
        String field = firstPresent(options, prefix + ".field", prefix + "Field");
        field = field == null ? shorthandValues.get("field") : field;
        if (field == null || field.isBlank()) {
            throw error(lineIndex, "Lookup field must not be blank");
        }
        String comparator = firstPresent(options, prefix + ".comparator", "comparator");
        comparator = comparator == null ? shorthandValues.getOrDefault("comparator", "EQUALS") : comparator;
        String column = firstPresent(options, prefix + ".column", prefix + "Column");
        column = column == null ? shorthandValues.get("column") : column;
        String nullSearch = firstPresent(options, prefix + ".nullSearch");
        nullSearch = nullSearch == null ? shorthandValues.get("nullSearch") : nullSearch;
        ImportLookup lookup = ImportLookup.builder()
                .field(field)
                .column(emptyToNull(column))
                .comparator(comparator == null || comparator.isBlank() ? "EQUALS" : comparator)
                .nullSearch(nullSearch != null && booleanValue(nullSearch))
                .metadata(new LinkedHashMap<>())
                .build();
        String metadataPrefix = prefix + ".metadata.";
        String metaPrefix = prefix + ".meta.";
        options.forEach((key, value) -> {
            if (key.startsWith(metadataPrefix) || key.startsWith(metaPrefix)) {
                lookup.getMetadata().put(metadataKey(removeAnyPrefix(key, metadataPrefix, metaPrefix)), metadataValue(value));
            }
        });
        return lookup;
    }

    /**
     * Parses lookup shorthand in the form
     * {@code field[:column[:comparator[:nullSearch]]]}.
     */
    private Map<String, String> parseLookupShorthand(String shorthand, int lineIndex) {
        Map<String, String> values = new LinkedHashMap<>();
        if (shorthand == null || shorthand.isBlank() || "true".equalsIgnoreCase(shorthand)) {
            return values;
        }
        List<String> parts = splitEscaped(shorthand, ':');
        if (parts.size() > 4) {
            throw error(lineIndex, "Lookup shorthand supports field[:column[:comparator[:nullSearch]]]");
        }
        values.put("field", parts.get(0).trim());
        if (parts.size() > 1) {
            values.put("column", parts.get(1).trim());
        }
        if (parts.size() > 2) {
            values.put("comparator", parts.get(2).trim());
        }
        if (parts.size() > 3) {
            values.put("nullSearch", parts.get(3).trim());
        }
        return values;
    }

    /**
     * Adds statement-level query parameters from query.params and explicit
     * query.param.* declarations.
     */
    private void addStatementQueryParams(ImportStatement statement, Map<String, String> options, int lineIndex) {
        String queryParams = firstPresent(options, "query.params", "queryParams");
        if (queryParams != null && !queryParams.isBlank()) {
            ensureQuery(statement);
            for (String paramSpec : splitOptions(queryParams)) {
                if (!paramSpec.isBlank()) {
                    addQueryParamSpec(statement, paramSpec.trim(), lineIndex);
                }
            }
        }
        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey().startsWith("query.param.") && !isQueryParamMetadataKey(entry.getKey())) {
                String paramName = entry.getKey().substring("query.param.".length()).trim();
                if (!paramName.isBlank() && !Set.of("name", "metadata", "meta").contains(paramName)) {
                    ensureQuery(statement);
                    statement.getQuery().getParams().put(paramName,
                            ImportQueryParam.builder().column(emptyToNull(entry.getValue()))
                                    .metadata(queryParamMetadata(options, paramName))
                                    .build());
                }
            }
        }
    }

    /**
     * Parses one query parameter shorthand item in the form
     * {@code paramName[:columnName]}.
     */
    private void addQueryParamSpec(ImportStatement statement, String spec, int lineIndex) {
        List<String> parts = splitEscaped(spec, ':');
        if (parts.isEmpty() || parts.size() > 2 || parts.get(0).trim().isBlank()) {
            throw error(lineIndex, "Query parameter shorthand supports paramName[:columnName]");
        }
        String paramName = parts.get(0).trim();
        String column = parts.size() == 2 ? emptyToNull(parts.get(1).trim()) : null;
        statement.getQuery().getParams().put(paramName,
                ImportQueryParam.builder().column(column).metadata(new LinkedHashMap<>()).build());
    }

    /**
     * Copies query-level metadata options into the import query model.
     */
    private void applyQueryMetadata(ImportStatement statement, Map<String, String> options) {
        if (statement.getQuery() == null) {
            return;
        }
        options.forEach((key, value) -> {
            if (key.startsWith("query.metadata.") || key.startsWith("query.meta.")) {
                statement.getQuery().getMetadata().put(metadataKey(removeAnyPrefix(key, "query.metadata.", "query.meta.")),
                        metadataValue(value));
            }
        });
    }

    /**
     * Extracts metadata for a specific query parameter.
     */
    private Map<String, Object> queryParamMetadata(Map<String, String> options, String paramName) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        String metadataPrefix = "query.param." + paramName + ".metadata.";
        String metaPrefix = "query.param." + paramName + ".meta.";
        options.forEach((key, value) -> {
            if (key.startsWith(metadataPrefix) || key.startsWith(metaPrefix)) {
                metadata.put(metadataKey(removeAnyPrefix(key, metadataPrefix, metaPrefix)), metadataValue(value));
            }
        });
        return metadata;
    }

    /**
     * Merges query parameter metadata into already-created parameter models.
     */
    private void applyQueryParamMetadata(ImportStatement statement, Map<String, String> options) {
        if (statement.getQuery() == null) {
            return;
        }
        for (Map.Entry<String, ImportQueryParam> entry : statement.getQuery().getParams().entrySet()) {
            entry.getValue().getMetadata().putAll(queryParamMetadata(options, entry.getKey()));
        }
    }

    /**
     * Initializes an empty query object when syntax declares params before query
     * text or at column level.
     */
    private void ensureQuery(ImportStatement statement) {
        if (statement.getQuery() == null) {
            statement.setQuery(ImportQuery.builder().metadata(new LinkedHashMap<>()).build());
        }
    }

    /**
     * Validates the minimum shape required before rows can be parsed for a
     * statement.
     */
    private void validateStatementShape(ImportStatement statement, int lineIndex) {
        if (statement.getEntity() == null || statement.getEntity().isBlank()) {
            throw error(lineIndex, "Statement entity must not be blank");
        }
        if (statement.getColumns().isEmpty()) {
            throw error(lineIndex, "Statement must declare at least one column");
        }
    }

    /**
     * Splits row/header cells on unescaped pipe characters.
     */
    private List<String> splitCells(String value) {
        return splitEscaped(value, '|');
    }

    /**
     * Splits option lists on unescaped comma characters.
     */
    private List<String> splitOptions(String value) {
        return splitEscaped(value, ',');
    }

    /**
     * Detects row and continuation-header lines that start with a pipe after
     * indentation.
     */
    private boolean startsWithCellSeparator(String line) {
        return line != null && line.stripLeading().startsWith("|");
    }

    /**
     * Splits a value on the active separator while preserving escapes meant for
     * later parser/import phases.
     */
    private List<String> splitEscaped(String value, char separator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        boolean quoted = false;
        char quote = '\0';
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (escaping) {
                if (character != separator && character != '\\') {
                    current.append('\\');
                }
                current.append(character);
                escaping = false;
            } else if (character == '\\') {
                escaping = true;
            } else if ((character == '"' || character == '\'') && (!quoted || quote == character)) {
                quoted = !quoted;
                quote = quoted ? character : '\0';
                current.append(character);
            } else if (character == separator && !quoted) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (escaping) {
            current.append('\\');
        }
        parts.add(current.toString());
        return parts;
    }

    /**
     * Determines whether all quotes opened in a logical line have been closed.
     */
    private boolean quotedTextClosed(CharSequence value) {
        boolean escaping = false;
        boolean quoted = false;
        char quote = '\0';
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (escaping) {
                escaping = false;
            } else if (character == '\\') {
                escaping = true;
            } else if ((character == '"' || character == '\'') && (!quoted || quote == character)) {
                quoted = !quoted;
                quote = quoted ? character : '\0';
            }
        }
        return !quoted;
    }

    /**
     * Detects whether a non-comment line begins with a supported import
     * operation.
     */
    private boolean startsWithOperation(String line) {
        int end = firstWhitespace(line);
        if (end < 0) {
            return false;
        }
        try {
            ImportOperation.valueOf(line.substring(0, end).toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Finds the first whitespace boundary after the operation token.
     */
    private int firstWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the end of the entity token before statement options begin.
     */
    private int entityEnd(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isWhitespace(character) || character == '(') {
                return i;
            }
        }
        return value.length();
    }

    /**
     * Finds a top-level separator outside quotes and parenthesized option groups.
     */
    private int separatorIndex(String value, char separator) {
        boolean escaping = false;
        boolean quoted = false;
        char quote = '\0';
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (escaping) {
                escaping = false;
            } else if (character == '\\') {
                escaping = true;
            } else if ((character == '"' || character == '\'') && (!quoted || quote == character)) {
                quoted = !quoted;
                quote = quoted ? character : '\0';
            } else if (!quoted && character == '(') {
                depth++;
            } else if (!quoted && character == ')' && depth > 0) {
                depth--;
            } else if (!quoted && depth == 0 && character == separator) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Identifies alias/macro declarations while excluding statement rows.
     */
    private boolean isMacroLine(String line) {
        return line.startsWith("&") && line.contains("=") && !line.contains("|");
    }

    /**
     * Parses a macro alias and optional macro metadata.
     */
    private void parseMacro(String line, ImportRequest request, int lineIndex) {
        int separator = line.indexOf('=');
        String key = line.substring(0, separator).trim();
        if (key.isBlank()) {
            throw error(lineIndex, "Macro key must not be blank");
        }
        String rawValue = line.substring(separator + 1).trim();
        String value = rawValue;
        Map<String, Object> metadata = new LinkedHashMap<>();
        int open = rawValue.indexOf('(');
        if (open > 0 && rawValue.endsWith(")")) {
            int close = matchingClose(rawValue, open);
            if (close == rawValue.length() - 1) {
                value = rawValue.substring(0, open).trim();
                parseOptions(rawValue.substring(open + 1, close), lineIndex)
                        .forEach((metadataKey, metadataValue) -> metadata.put(metadataKey(metadataKey),
                                metadataValue(metadataValue)));
            }
        }
        request.getMacros().put(key, ImportMacro.builder().value(value).metadata(metadata).build());
    }

    /**
     * Resolves an entity alias to its full entity class name.
     */
    private String resolveAlias(String value, ImportRequest request) {
        if (value.startsWith("&") && request.getMacros().containsKey(value)) {
            return request.getMacros().get(value).getValue();
        }
        return value;
    }

    /**
     * Detects whole-cell quotes so explicit empty strings can be preserved.
     */
    private boolean isQuoted(String value) {
        return value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")));
    }

    /**
     * Identifies column options consumed by the parser instead of copied into
     * column metadata.
     */
    private boolean knownColumnOption(String key) {
        return List.of("unique", "nullSearch", "default", "defaultValue", "mode", "reference", "handler",
                "lookup", "lookup.field", "lookupField", "lookup.column", "lookupColumn",
                "lookup.comparator", "comparator", "lookup.nullSearch", "queryParam", "query.param",
                "queryParam.name", "query.param.name").contains(key)
                || key.startsWith("lookup.metadata.") || key.startsWith("lookup.meta.")
                || key.startsWith("queryParam.metadata.") || key.startsWith("queryParam.meta.")
                || key.startsWith("query.param.metadata.") || key.startsWith("query.param.meta.");
    }

    /**
     * Identifies statement options consumed by the parser instead of copied into
     * statement metadata.
     */
    private boolean knownStatementOption(String key) {
        return List.of("query", "query.text", "query.params", "queryParams", "lookup", "lookup.field",
                "lookupField", "lookup.column", "lookupColumn", "lookup.comparator", "lookup.nullSearch",
                "comparator").contains(key)
                || key.startsWith("lookup.") || key.startsWith("query.param.")
                || key.startsWith("query.metadata.") || key.startsWith("query.meta.");
    }

    /**
     * Reads boolean options where a key without value means true.
     */
    private boolean booleanOption(Map<String, String> options, String key, boolean defaultValue) {
        if (!options.containsKey(key)) {
            return defaultValue;
        }
        String value = options.get(key);
        return value == null || value.isBlank() || Boolean.parseBoolean(value);
    }

    /**
     * Returns the first configured option value from a list of aliases.
     */
    private String firstPresent(Map<String, String> options, String... keys) {
        for (String key : keys) {
            if (options.containsKey(key)) {
                return options.get(key);
            }
        }
        return null;
    }

    /**
     * Removes matching whole-value quotes from option and row values.
     */
    private String unquote(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Converts blank unquoted values to null for the import model.
     */
    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Converts obvious metadata scalar values to booleans or whole numbers.
     */
    private Object metadataValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return Boolean.parseBoolean(trimmed);
        }
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException ignored) {
        }
        return value;
    }

    /**
     * Normalizes {@code metadata.} and {@code meta.} keys to their stored key
     * names.
     */
    private String metadataKey(String key) {
        return removeAnyPrefix(key, "metadata.", "meta.");
    }

    /**
     * Removes the first matching prefix from a value.
     */
    private String removeAnyPrefix(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length());
            }
        }
        return value;
    }

    /**
     * Checks whether any option key exists in a parsed option map.
     */
    private boolean containsAny(Map<String, String> options, String... keys) {
        for (String key : keys) {
            if (options.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Discovers indexed lookup groups such as {@code lookup.1.field}.
     */
    private List<String> lookupIndexes(Map<String, String> options) {
        List<String> indexes = new ArrayList<>();
        for (String key : options.keySet()) {
            if (key.startsWith("lookup.")) {
                String remaining = key.substring("lookup.".length());
                int dot = remaining.indexOf('.');
                if (dot > 0) {
                    String index = remaining.substring(0, dot);
                    if (index.chars().allMatch(Character::isDigit) && !indexes.contains(index)) {
                        indexes.add(index);
                    }
                }
            }
        }
        return indexes;
    }

    /**
     * Distinguishes query parameter metadata keys from query parameter source
     * declarations.
     */
    private boolean isQueryParamMetadataKey(String key) {
        return key.contains(".metadata.") || key.contains(".meta.");
    }

    /**
     * Parses integer request options with parser-line context on failure.
     */
    private int intOption(String value, String key, int lineIndex) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw error(lineIndex, "Option '" + key + "' must be an integer");
        }
    }

    /**
     * Parses boolean option values where blank means true.
     */
    private boolean booleanValue(String value) {
        return value == null || value.isBlank() || Boolean.parseBoolean(value);
    }

    /**
     * Finds the closing parenthesis that matches an opening parenthesis while
     * ignoring parentheses inside quotes.
     */
    private int matchingClose(String value, int open) {
        boolean escaping = false;
        boolean quoted = false;
        char quote = '\0';
        int depth = 0;
        for (int i = open; i < value.length(); i++) {
            char character = value.charAt(i);
            if (escaping) {
                escaping = false;
            } else if (character == '\\') {
                escaping = true;
            } else if ((character == '"' || character == '\'') && (!quoted || quote == character)) {
                quoted = !quoted;
                quote = quoted ? character : '\0';
            } else if (!quoted && character == '(') {
                depth++;
            } else if (!quoted && character == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Removes a UTF-8 byte-order mark from the first line when present.
     */
    private String stripBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    /**
     * Creates a parser exception with one-based line information.
     */
    private CoredeuxImportParserException error(int lineIndex, String message) {
        return new CoredeuxImportParserException("Line " + (lineIndex + 1) + ": " + message);
    }

    /**
     * Parsed header name and option map.
     */
    private record HeaderSpec(String name, Map<String, String> options) {
    }

    /**
     * Parsed row key and metadata map.
     */
    private record RowKeySpec(String key, Map<String, Object> metadata) {
    }

    /**
     * Logical line text plus its original starting physical line.
     */
    private record SourceLine(String text, int lineIndex) {
    }
}
