package com.coredeux.export.handler;

import com.coredeux.export.model.ExportField;
import com.coredeux.export.model.ExportRequest;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExportValueContext {

    private final Object rootEntity;
    private final Object resolvedValue;
    private final String fieldPath;
    private final Class<?> entityType;
    private final ExportField field;
    private final ExportRequest request;
}
