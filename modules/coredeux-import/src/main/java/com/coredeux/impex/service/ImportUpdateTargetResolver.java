package com.coredeux.impex.service;

/** Resolves an existing import update target before any fields are populated. */
@FunctionalInterface
public interface ImportUpdateTargetResolver {
    Object resolve(Object entity);
}
