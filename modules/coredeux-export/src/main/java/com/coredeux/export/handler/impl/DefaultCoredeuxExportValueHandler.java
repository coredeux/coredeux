package com.coredeux.export.handler.impl;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import com.coredeux.export.handler.CoredeuxExportValueHandler;
import com.coredeux.export.handler.ExportValueContext;

public class DefaultCoredeuxExportValueHandler implements CoredeuxExportValueHandler {

    @Override
    public Object handle(ExportValueContext context) {
        Object value = context.getResolvedValue();
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
}
