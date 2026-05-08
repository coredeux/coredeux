package com.coredeux.impex.service.impl;

import com.coredeux.impex.model.ImportStatement;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImportEntityMetadata {

    Class<?> targetClass;
    String identifierPath;
    ImportStatement statement;
}
