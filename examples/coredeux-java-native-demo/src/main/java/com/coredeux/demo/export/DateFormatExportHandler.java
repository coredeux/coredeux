package com.coredeux.demo.export;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;


import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.handler.CoredeuxExportValueHandler;
import com.coredeux.export.handler.ExportValueContext;

public class DateFormatExportHandler implements CoredeuxExportValueHandler {

    private static final String DATE_FORMAT_KEY = "dateFormat";
    private static final String TIMEZONE_KEY = "timezone";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

    @Override
    public Object handle(ExportValueContext context) {
        Object value = context.getResolvedValue();
        if (value == null) {
            return "";
        }
        if (!(value instanceof Date date)) {
            throw new CoredeuxExportException("dateFormatExportHandler supports java.util.Date values only");
        }

        Map<String, Object> metadata = context.getField() == null ? null : context.getField().getMetadata();
        String pattern = stringValue(metadata == null ? null : metadata.get(DATE_FORMAT_KEY));
        if (pattern == null || pattern.isBlank()) {
            throw new CoredeuxExportException("Missing metadata.dateFormat for export field: " + context.getFieldPath());
        }

        ZoneId zone = resolveZone(metadata);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern.trim()).withZone(zone);
            return formatter.format(date.toInstant());
        } catch (IllegalArgumentException exception) {
            throw new CoredeuxExportException("Invalid metadata.dateFormat for date export handler: " + pattern,
                    exception);
        }
    }

    private ZoneId resolveZone(Map<String, Object> metadata) {
        String timezone = stringValue(metadata == null ? null : metadata.get(TIMEZONE_KEY));
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            throw new CoredeuxExportException("Invalid metadata.timezone for date export handler: " + timezone,
                    exception);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
