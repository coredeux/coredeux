package com.coredeux.examples.nativejava;

import java.lang.reflect.Field;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;

final class NativeEntityResolver {

    private final EntityDefinitionRegistry entityDefinitionRegistry;
    private final CoredeuxReflectionHelperService reflectionHelperService;

    NativeEntityResolver(EntityDefinitionRegistry entityDefinitionRegistry,
            CoredeuxReflectionHelperService reflectionHelperService) {
        this.entityDefinitionRegistry = entityDefinitionRegistry;
        this.reflectionHelperService = reflectionHelperService;
    }

    CoredeuxEntityDefinition resolveDefinition(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            throw new CoredeuxValidationException("Entity name must not be blank");
        }
        return entityDefinitionRegistry.findByFullClassName(entityName.trim())
                .orElseThrow(() -> new CoredeuxValidationException("Unknown entity: " + entityName));
    }

    Class<?> resolveType(String entityName) {
        return reflectionHelperService.getClass(resolveDefinition(entityName).getFullClassName());
    }

    Object getIdentifierValue(Object entity, CoredeuxEntityDefinition definition) {
        return reflectionHelperService.getFieldValue(definition.getIdentifier(), entity);
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
        return reflectionHelperService.getDeclaredField(definition.getIdentifier(), entityType);
    }
}
