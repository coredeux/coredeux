package com.coredeux.export.handler.impl;

import com.coredeux.export.handler.CoredeuxExportValueHandler;
import com.coredeux.export.handler.ExportValueContext;
import com.coredeux.export.support.ExportValueSupport;

public class DefaultCoredeuxExportValueHandler implements CoredeuxExportValueHandler {

    @Override
    public Object handle(ExportValueContext context) {
        return ExportValueSupport.convert(context.getResolvedValue());
    }
}
