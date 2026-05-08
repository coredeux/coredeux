package com.coredeux.core.resolver;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;

/**
 * Default resolver that reads the data access service name directly from the
 * entity definition storage section.
 */
@Service
public class EntityDefinitionBackedDataAccessResolver implements EntityDataAccessResolver {

    @Override
    public String resolveDataAccessService(CoredeuxEntityDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        if (definition.getStorage() == null || definition.getStorage().getDataAccessService() == null
                || definition.getStorage().getDataAccessService().isBlank()) {
            throw new CoredeuxValidationException(
                    "No data access service configured for entity class: " + definition.getFullClassName());
        }
        return definition.getStorage().getDataAccessService();
    }
}
