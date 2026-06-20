package com.coredeux.core.resolver.impl;

import java.util.List;
import java.util.Objects;

import com.coredeux.core.config.CoredeuxProperties;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.registry.EntityDefinitionRegistry;
import com.coredeux.core.resolver.CoredeuxEntityDefinitionResolver;

/**
 * Default resolver that normalizes configured entity definitions and provides a
 * synthesized fallback when none is configured.
 */
public class DefaultCoredeuxEntityDefinitionResolver implements CoredeuxEntityDefinitionResolver {

	private final EntityDefinitionRegistry entityDefinitionRegistry;
	private final CoredeuxProperties coredeuxProperties;

	public DefaultCoredeuxEntityDefinitionResolver(EntityDefinitionRegistry entityDefinitionRegistry,
			CoredeuxProperties coredeuxProperties) {
		this.entityDefinitionRegistry = entityDefinitionRegistry;
		this.coredeuxProperties = coredeuxProperties == null ? new CoredeuxProperties() : coredeuxProperties;
	}

	@Override
	public <T> CoredeuxEntityDefinition resolve(Class<T> entityType) {
		CoredeuxEntityDefinition definition = entityDefinitionRegistry.findByEntityType(entityType)
				.orElseGet(() -> defaultDefinition(entityType));
		return normalizeDefinition(definition);
	}

	private <T> CoredeuxEntityDefinition defaultDefinition(Class<T> entityType) {
		CoredeuxStorageDefinition storage = effectiveStorage(null);
		return CoredeuxEntityDefinition.builder().fullClassName(entityType.getName()).name(entityType.getSimpleName())
				.identifier(effectiveIdentifier(null)).storage(storage).modules(List.of()).build();
	}

	private CoredeuxEntityDefinition normalizeDefinition(CoredeuxEntityDefinition definition) {
		String identifier = effectiveIdentifier(definition);
		CoredeuxStorageDefinition storage = effectiveStorage(definition.getStorage());

		if (Objects.equals(definition.getIdentifier(), identifier)
				&& Objects.equals(storage, definition.getStorage())) {
			return definition;
		}

		return definition.toBuilder().identifier(identifier).storage(storage).build();
	}

	private CoredeuxStorageDefinition effectiveStorage(CoredeuxStorageDefinition storage) {
		String fallback = globalDataAccessService();
		if (fallback == null || fallback.isBlank()) {
			return storage;
		}

		if (storage == null) {
			return CoredeuxStorageDefinition.builder().dataAccessService(fallback).build();
		}

		if (storage.getDataAccessService() == null || storage.getDataAccessService().isBlank()) {
			return storage.toBuilder().dataAccessService(fallback).build();
		}
		return storage;
	}

	private String effectiveIdentifier(CoredeuxEntityDefinition definition) {
		if (definition != null) {
			if (definition.getIdentifier() != null && !definition.getIdentifier().isBlank()) {
				return definition.getIdentifier();
			}

			CoredeuxStorageDefinition storage = definition.getStorage();
			if (storage != null && storage.getIdentifier() != null && !storage.getIdentifier().isBlank()) {
				return storage.getIdentifier();
			}
		}
		return globalIdentifier();
	}

	private String globalDataAccessService() {
		return coredeuxProperties.string("data-access-service");
	}

	private String globalIdentifier() {
		return coredeuxProperties.string("identifier");
	}
}
