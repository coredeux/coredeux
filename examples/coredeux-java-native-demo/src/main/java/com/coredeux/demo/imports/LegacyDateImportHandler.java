package com.coredeux.demo.imports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.handler.CoredeuxImportValueHandler;
import com.coredeux.impex.handler.ImportValueContext;

public class LegacyDateImportHandler implements CoredeuxImportValueHandler {

    @Override
    public Object handle(ImportValueContext context) {
        String value = context.getEffectiveValue();
        String columnName = context.getColumn() == null ? null : context.getColumn().getName();
        if (value == null || value.isBlank()) {
            return null;
        }
        if (context.getExpectedType() != null && !Date.class.isAssignableFrom(context.getExpectedType())) {
            throw new CoredeuxImportException("legacyDateImportHandler supports java.util.Date targets only", columnName);
        }

        Map<String, Object> metadata = context.getColumn() == null ? null : context.getColumn().getMetadata();
        String pattern = metadata == null ? null : stringValue(metadata.get("dateFormat"));
        if (pattern == null || pattern.isBlank()) {
            throw new CoredeuxImportException("Missing metadata.dateFormat for legacy date column: " + columnName, columnName);
        }

        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
        format.setLenient(false);
        String timezone = metadata == null ? null : stringValue(metadata.get("timezone"));
        if (timezone != null && !timezone.isBlank()) {
            format.setTimeZone(TimeZone.getTimeZone(timezone));
        }
        try {
            return format.parse(value.trim());
        } catch (ParseException exception) {
            throw new CoredeuxImportException("Invalid date value for column: " + columnName, columnName, exception);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
