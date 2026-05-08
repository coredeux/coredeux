package com.coredeux.export.service.impl;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import org.springframework.stereotype.Component;

@Component
class ExportValueFormatter {

    String format(Object value) {
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
        return String.valueOf(value);
    }
}
