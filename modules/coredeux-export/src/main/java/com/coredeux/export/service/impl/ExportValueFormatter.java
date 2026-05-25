package com.coredeux.export.service.impl;

import com.coredeux.export.support.ExportValueSupport;

public class ExportValueFormatter {

    public String format(Object value) {
        return ExportValueSupport.format(value);
    }
}
