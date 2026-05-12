package com.coredeux.core.registry;

/**
 * Resolves named framework components such as data access services, validators,
 * hooks, and audit handlers.
 */
public interface CoredeuxComponentRegistry {

    /**
     * Resolves a named component and verifies that it implements the requested
     * contract.
     *
     * @param name component name
     * @param type expected component contract
     * @param <T> component contract type
     * @return resolved component
     */
    <T> T getComponent(String name, Class<T> type);
}
