package com.coredeux.demo.web;

import java.lang.reflect.Field;

import org.springframework.stereotype.Component;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.registry.EntityDefinitionRegistry;

@Component
public class DemoEntityResolver {

    private final EntityDefinitionRegistry entityDefinitionRegistry;
    private final CoredeuxReflectionHelperService reflectionHelperService;

    public DemoEntityResolver(EntityDefinitionRegistry entityDefinitionRegistry,
            CoredeuxReflectionHelperService reflectionHelperService) {
        this.entityDefinitionRegistry = entityDefinitionRegistry;
        this.reflectionHelperService = reflectionHelperService;
    }

    public CoredeuxEntityDefinition resolveDefinition(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            throw new CoredeuxValidationException("Entity name must not be blank");
        }
        return entityDefinitionRegistry.findByFullClassName(entityName.trim())
                .orElseThrow(() -> new CoredeuxValidationException("Unknown entity: " + entityName));
    }

    public Class<?> resolveType(String entityName) {
        return reflectionHelperService.getClass(resolveDefinition(entityName).getFullClassName());
    }

    public Class<?> resolveIdentifierType(Class<?> entityType, CoredeuxEntityDefinition definition) {
        Field identifierField = reflectionHelperService.getDeclaredField(definition.getIdentifier(), entityType);
        if (identifierField == null) {
            throw new CoredeuxValidationException(
                    "Unable to resolve identifier field '" + definition.getIdentifier() + "' on class: "
                            + entityType.getName());
        }
        return identifierField.getType();
    }

    public void applyIdentifier(Object entity, CoredeuxEntityDefinition definition, Object identifierValue) {
        Field identifierField = reflectionHelperService.getDeclaredField(definition.getIdentifier(), entity.getClass());
        if (identifierField == null) {
            throw new CoredeuxValidationException(
                    "Unable to resolve identifier field '" + definition.getIdentifier() + "' on class: "
                            + entity.getClass().getName());
        }
        reflectionHelperService.setFieldValue(identifierField, entity, identifierValue);
    }
}
