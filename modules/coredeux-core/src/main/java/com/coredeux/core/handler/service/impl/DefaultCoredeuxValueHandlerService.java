package com.coredeux.core.handler.service.impl;

import java.util.Objects;

import com.coredeux.core.exceptions.CoredeuxValueHandlerException;
import com.coredeux.core.handler.CoredeuxValueHandler;
import com.coredeux.core.handler.ValueContext;
import com.coredeux.core.handler.service.CoredeuxValueHandlerService;
import com.coredeux.core.registry.CoredeuxComponentRegistry;

public class DefaultCoredeuxValueHandlerService implements CoredeuxValueHandlerService {

	private final CoredeuxComponentRegistry componentRegistry;

	public DefaultCoredeuxValueHandlerService(CoredeuxComponentRegistry componentRegistry) {
		this.componentRegistry = componentRegistry;
	}

	@Override
	public <T, V extends ValueContext> T invoke(String handler, V context) {
		V resolvedContext = Objects.requireNonNull(context, "Value handler context must not be null");
		String handlername = resolveHandlerName(handler);
		CoredeuxComponentRegistry registry = this.componentRegistry;
		if (registry == null) {
			throw new CoredeuxValueHandlerException(
					"Value handler service does not provide a component registry for handler: " + handlername);
		}

		CoredeuxValueHandler<T, V> valueHandler = resolveHandler(registry, handlername);
		return valueHandler.handle(resolvedContext);
	}

	@SuppressWarnings("unchecked")
	protected <T, V extends ValueContext> CoredeuxValueHandler<T, V> resolveHandler(CoredeuxComponentRegistry registry,
			String handler) {
		try {
			return (CoredeuxValueHandler<T, V>) registry.getComponent(handler, CoredeuxValueHandler.class);
		} catch (RuntimeException exception) {
			throw new CoredeuxValueHandlerException("Unable to resolve value handler: " + handler, exception);
		}
	}

	protected <V extends ValueContext> String resolveHandlerName(String handler) {
		if (handler != null && !handler.isBlank()) {
			return handler.trim();
		}
		throw new CoredeuxValueHandlerException("Value handler name is required");
	}
}
