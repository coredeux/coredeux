package com.coredeux.core.strategy;

/**
 * Canonical lifecycle operation values used across Coredeux modules.
 */
public final class CoredeuxLifecycleOperations {

    public static final String CREATE = "CREATE";
    public static final String MODIFY = "MODIFY";
    public static final String UPSERT = "UPSERT";
    public static final String DELETE = "DELETE";
    public static final String FETCH = "FETCH";

    private CoredeuxLifecycleOperations() {
    }
}
