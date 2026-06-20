package com.coredeux.examples.nativejava;

import java.lang.reflect.Field;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.resolver.CoredeuxEntityDefinitionResolver;

final class NativeEntityResolver {

    private final CoredeuxReflectionHelperService reflectionHelperService;
    private final CoredeuxEntityDefinitionResolver entityDefinitionResolver;

    NativeEntityResolver(CoredeuxReflectionHelperService reflectionHelperService,
            CoredeuxEntityDefinitionResolver entityDefinitionResolver) {
        this.reflectionHelperService = reflectionHelperService;
        this.entityDefinitionResolver = entityDefinitionResolver;
    }

    CoredeuxEntityDefinition resolveDefinition(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            throw new CoredeuxValidationException("Entity name must not be blank");
        }
        Class<?> entityType = reflectionHelperService.getClass(entityName.trim());
        return entityDefinitionResolver.resolve(entityType);
    }

    Class<?> resolveType(String entityName) {
        return reflectionHelperService.getClass(resolveDefinition(entityName).getFullClassName());
    }

    Object getIdentifierValue(Object entity, CoredeuxEntityDefinition definition) {
        return reflectionHelperService.getFieldValue(resolveIdentifierFieldName(entity.getClass(), definition), entity);
    }

    void applyIdentifier(Object entity, CoredeuxEntityDefinition definition, Object identifierValue) {
        Field identifierField = getIdentifierField(entity.getClass(), definition);
        if (identifierField == null) {
            throw new CoredeuxValidationException("Unable to resolve identifier field '" + definition.getIdentifier()
                    + "' on class: " + entity.getClass().getName());
        }
        reflectionHelperService.setFieldValue(identifierField, entity, identifierValue);
    }

    Class<?> resolveIdentifierType(Class<?> entityType, CoredeuxEntityDefinition definition) {
        Field identifierField = getIdentifierField(entityType, definition);
        if (identifierField == null) {
            throw new CoredeuxValidationException("Unable to resolve identifier field '" + definition.getIdentifier()
                    + "' on class: " + entityType.getName());
        }
        return identifierField.getType();
    }

    private Field getIdentifierField(Class<?> entityType, CoredeuxEntityDefinition definition) {
        return reflectionHelperService.getDeclaredField(resolveIdentifierFieldName(entityType, definition), entityType);
    }

    private String resolveIdentifierFieldName(Class<?> entityType, CoredeuxEntityDefinition definition) {
        CoredeuxEntityDefinition effectiveDefinition = entityDefinitionResolver.resolve(entityType);
        if (definition != null && definition.getIdentifier() != null && !definition.getIdentifier().isBlank()) {
            return definition.getIdentifier();
        }
        return effectiveDefinition.getIdentifier();
    }
}
