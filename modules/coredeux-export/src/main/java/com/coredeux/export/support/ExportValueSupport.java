package com.coredeux.export.support;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * Shared export-value normalization and formatting rules.
 */
public final class ExportValueSupport {

    private ExportValueSupport() {
    }

    public static Object convert(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Date date) {
            return date.toInstant().toString();
        }
        if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return value;
    }

    public static String format(Object value) {
        Object converted = convert(value);
        return converted == null ? "" : String.valueOf(converted);
    }
}
