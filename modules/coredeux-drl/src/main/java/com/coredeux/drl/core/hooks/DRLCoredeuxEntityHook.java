package com.coredeux.drl.core.hooks;

import com.coredeux.drl.model.RuleContext;

/**
 * Extension point for DRL-backed entity lifecycle hooks resolved from
 * configuration.
 *
 * <p>Each method represents a lifecycle phase and receives a mutable rule
 * context carrying the entity, definition, and operation metadata.
 */
public interface DRLCoredeuxEntityHook<T> {

    /**
     * Invoked when the entity is loaded.
     *
     * @param $context the rule execution context for the load phase
     */
	void onLoad(RuleContext<T> $context);

    /**
     * Invoked before the entity is saved.
     *
     * @param $context the rule execution context for the before-save phase
     */
	void beforeSave(RuleContext<T> $context);

    /**
     * Invoked after the entity is saved.
     *
     * @param $context the rule execution context for the after-save phase
     */
	void afterSave(RuleContext<T> $context);

    /**
     * Invoked before the entity is updated.
     *
     * @param $context the rule execution context for the before-update phase
     */
	void beforeUpdate(RuleContext<T> $context);

    /**
     * Invoked after the entity is updated.
     *
     * @param $context the rule execution context for the after-update phase
     */
	void afterUpdate(RuleContext<T> $context);

    /**
     * Invoked before the entity is deleted.
     *
     * @param $context the rule execution context for the before-delete phase
     */
	void beforeDelete(RuleContext<T> $context);

    /**
     * Invoked before the entity is refreshed.
     *
     * @param $context the rule execution context for the before-refresh phase
     */
	void beforeRefresh(RuleContext<T> $context);

    /**
     * Invoked after the entity is refreshed.
     *
     * @param $context the rule execution context for the after-refresh phase
     */
	void afterRefresh(RuleContext<T> $context);
}
