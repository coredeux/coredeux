package com.coredeux.demo.imports;

import java.net.URI;

import org.springframework.stereotype.Component;

import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.handler.CoredeuxImportValueHandler;
import com.coredeux.impex.handler.ImportValueContext;

@Component("demoUriImportHandler")
public class DemoUriImportHandler implements CoredeuxImportValueHandler {

    @Override
    public Object handle(ImportValueContext context) {
        String value = context.getEffectiveValue();
        String columnName = context.getColumn() == null ? null : context.getColumn().getName();
        if (value == null || value.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new CoredeuxImportException("Invalid URI value for column: " + columnName, columnName, exception);
        }
        if (!uri.isAbsolute()) {
            throw new CoredeuxImportException("URI value must be absolute for column: " + columnName, columnName);
        }
        return uri;
    }
}
