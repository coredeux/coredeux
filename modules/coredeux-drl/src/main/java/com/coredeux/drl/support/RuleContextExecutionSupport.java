package com.coredeux.drl.support;

import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.exceptions.CoredeuxDRLException;

/**
 * Shared guard for DRL executions that communicate results through a
 * {@link RuleContext}.
 *
 * <p>The DRL runtime can populate {@link RuleContext#setException(Exception)}
 * instead of throwing directly. Callers should use this helper immediately
 * after execution to make sure rule failures are surfaced instead of being
 * treated like successful executions with a {@code null} output.
 */
public final class RuleContextExecutionSupport {

    private RuleContextExecutionSupport() {
    }

    /**
     * Throws a {@link CoredeuxDRLException} when the supplied context carries a
     * rule exception.
     *
     * @param context the execution context to inspect
     * @param executionLabel human-readable label included in the failure message
     */
    public static void throwIfException(RuleContext<?> context, String executionLabel) {
        if (context != null && context.getException() != null) {
            throw new CoredeuxDRLException(executionLabel + " captured an exception", context.getException());
        }
    }

    /**
     * Returns the context output after first checking for a captured exception.
     *
     * @param context the execution context to inspect
     * @param executionLabel human-readable label included in the failure message
     * @param <T> the output type
     * @return the context output when execution completed normally
     */
    public static <T> T outputOrThrow(RuleContext<T> context, String executionLabel) {
        throwIfException(context, executionLabel);
        return context != null ? context.getOutput() : null;
    }
}
