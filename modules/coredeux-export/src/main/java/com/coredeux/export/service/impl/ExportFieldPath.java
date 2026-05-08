package com.coredeux.export.service.impl;

import java.util.List;

import com.coredeux.export.model.ExportField;

record ExportFieldPath(String expression, List<String> segments, ExportField field) {
}
