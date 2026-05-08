package com.coredeux.core.hooks;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;

/**
 * Extension point for entity lifecycle hooks resolved from configuration.
 */
public interface CoredeuxEntityHook<T> {

    default void onLoad(T entity, CoredeuxEntityDefinition definition, OperationContext context) {
    }

    default void beforeSave(T entity, CoredeuxEntityDefinition definition, OperationContext context) {
    }

    default void afterSave(T entity, CoredeuxEntityDefinition definition, OperationContext context) {
    }

    default void beforeUpdate(T entity, CoredeuxEntityDefinition definition, OperationContext context) {
    }

    default void afterUpdate(T entity, CoredeuxEntityDefinition definition, OperationContext context) {
    }

    default void beforeDelete(T entity, CoredeuxEntityDefinition definition, OperationContext context) {
    }

    default void beforeRefresh(T entity, CoredeuxEntityDefinition definition, OperationContext context) {
    }

    default void afterRefresh(T entity, CoredeuxEntityDefinition definition, OperationContext context) {
    }
}
