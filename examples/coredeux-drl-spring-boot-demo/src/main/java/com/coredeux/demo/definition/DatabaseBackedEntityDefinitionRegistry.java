package com.coredeux.demo.definition;

import java.util.Collection;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.registry.EntityDefinitionRegistry;

/**
 * Database-backed entity definition registry with Redis as a refreshable cache.
 */
@Service
@Primary
public class DatabaseBackedEntityDefinitionRegistry implements EntityDefinitionRegistry {

    private final EntityDefinitionManager manager;

    public DatabaseBackedEntityDefinitionRegistry(EntityDefinitionManager manager) {
		this.manager = manager;
	}

    @Override
    public Optional<CoredeuxEntityDefinition> findByFullClassName(String fullClassName) {
        return manager.currentRegistry().findByFullClassName(fullClassName);
    }

    @Override
    public Collection<CoredeuxEntityDefinition> getAll() {
        return manager.currentRegistry().getAll();
    }
}
