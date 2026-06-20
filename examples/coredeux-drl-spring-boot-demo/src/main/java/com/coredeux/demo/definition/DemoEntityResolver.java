package com.coredeux.demo.definition;

import java.lang.reflect.Field;

import org.springframework.stereotype.Component;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.helper.CoredeuxReflectionHelperService;
import com.coredeux.core.resolver.CoredeuxEntityDefinitionResolver;

@Component
public class DemoEntityResolver {

    private final CoredeuxReflectionHelperService reflectionHelperService;
    private final CoredeuxEntityDefinitionResolver entityDefinitionResolver;

    public DemoEntityResolver(CoredeuxReflectionHelperService reflectionHelperService,
            CoredeuxEntityDefinitionResolver entityDefinitionResolver) {
        this.reflectionHelperService = reflectionHelperService;
        this.entityDefinitionResolver = entityDefinitionResolver;
    }

    public CoredeuxEntityDefinition resolveDefinition(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            throw new CoredeuxValidationException("Entity name must not be blank");
        }
        Class<?> entityType = reflectionHelperService.getClass(entityName.trim());
        return entityDefinitionResolver.resolve(entityType);
    }

    public Class<?> resolveType(String entityName) {
        return reflectionHelperService.getClass(resolveDefinition(entityName).getFullClassName());
    }

    public Class<?> resolveIdentifierType(Class<?> entityType, CoredeuxEntityDefinition definition) {
        Field identifierField = reflectionHelperService.getDeclaredField(resolveIdentifierFieldName(entityType, definition),
                entityType);
        if (identifierField == null) {
            throw new CoredeuxValidationException(
                    "Unable to resolve identifier field '" + definition.getIdentifier() + "' on class: "
                            + entityType.getName());
        }
        return identifierField.getType();
    }

    public void applyIdentifier(Object entity, CoredeuxEntityDefinition definition, Object identifierValue) {
        Field identifierField = reflectionHelperService.getDeclaredField(resolveIdentifierFieldName(entity.getClass(),
                definition), entity.getClass());
        if (identifierField == null) {
            throw new CoredeuxValidationException(
                    "Unable to resolve identifier field '" + definition.getIdentifier() + "' on class: "
                            + entity.getClass().getName());
        }
        reflectionHelperService.setFieldValue(identifierField, entity, identifierValue);
    }

    private String resolveIdentifierFieldName(Class<?> entityType, CoredeuxEntityDefinition definition) {
        CoredeuxEntityDefinition effectiveDefinition = entityDefinitionResolver.resolve(entityType);
        if (definition != null && definition.getIdentifier() != null && !definition.getIdentifier().isBlank()) {
            return definition.getIdentifier();
        }
        return effectiveDefinition.getIdentifier();
    }
}
