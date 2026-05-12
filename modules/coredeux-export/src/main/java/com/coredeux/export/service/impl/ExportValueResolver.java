package com.coredeux.export.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.export.exception.CoredeuxExportException;
import com.coredeux.export.handler.ExportValueContext;
import com.coredeux.export.handler.ExportValueHandlerResolver;
import com.coredeux.export.model.ExportRequest;

public class ExportValueResolver {

    private final CoredeuxReflectionHelperService reflectionHelperService;
    private final ExportValueFormatter formatter;
    private final ExportValueHandlerResolver handlerResolver;

    public ExportValueResolver(CoredeuxReflectionHelperService reflectionHelperService, ExportValueFormatter formatter,
            ExportValueHandlerResolver handlerResolver) {
        this.reflectionHelperService = reflectionHelperService;
        this.formatter = formatter;
        this.handlerResolver = handlerResolver;
    }

    public String resolve(Object root, Class<?> entityType, ExportFieldPath path, String collectionSeparator,
            ExportRequest request) {
        Object value = resolveValue(root, root == null ? null : root.getClass(), path.segments(), 0, false,
                collectionSeparator);
        Object handled = handlerResolver.resolve(path.field().getHandler())
                .handle(ExportValueContext.builder()
                        .rootEntity(root)
                        .resolvedValue(value)
                        .fieldPath(path.expression())
                        .entityType(entityType)
                        .field(path.field())
                        .request(request)
                        .build());
        return formatter.format(handled);
    }

    private Object resolveValue(Object instance, Class<?> type, List<String> segments, int index,
            boolean collectionAlreadySeen, String collectionSeparator) {
        if (instance == null) {
            return null;
        }
        if (index >= segments.size()) {
            return instance;
        }
        Object value = readSegment(instance, type, segments.get(index));
        if (value instanceof Collection<?> collection) {
            if (collectionAlreadySeen) {
                throw new CoredeuxExportException("Nested collection export paths are not supported: "
                        + String.join(":", segments));
            }
            return flattenCollection(collection, segments, index + 1, collectionSeparator);
        }
        if (value instanceof Map<?, ?> map && index + 1 < segments.size()) {
            return resolveValue(map.get(segments.get(index + 1)), null, segments, index + 2, collectionAlreadySeen,
                    collectionSeparator);
        }
        return resolveValue(value, value == null ? null : value.getClass(), segments, index + 1, collectionAlreadySeen,
                collectionSeparator);
    }

    private Object readSegment(Object instance, Class<?> type, String segment) {
        if (instance instanceof Map<?, ?> map) {
            return map.get(segment);
        }
        Class<?> targetType = type == null ? instance.getClass() : type;
        if (reflectionHelperService.getDeclaredField(segment, targetType) == null) {
            throw new CoredeuxExportException("Unable to resolve export field '" + segment + "' on "
                    + targetType.getName());
        }
        return reflectionHelperService.getFieldValue(segment, instance, targetType);
    }

    private String flattenCollection(Collection<?> collection, List<String> segments, int nextIndex,
            String collectionSeparator) {
        List<String> values = new ArrayList<>();
        for (Object element : collection) {
            Object value = nextIndex >= segments.size()
                    ? element
                    : resolveValue(element, element == null ? null : element.getClass(), segments, nextIndex, true,
                            collectionSeparator);
            String formatted = formatter.format(value);
            if (!formatted.isBlank()) {
                values.add(formatted);
            }
        }
        return String.join(collectionSeparator == null ? ", " : collectionSeparator, values);
    }
}
