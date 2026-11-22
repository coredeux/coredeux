package com.coredeux.drl.service;

import java.util.List;
import java.util.Map;

import com.coredeux.drl.cache.CompiledDRLRule;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.support.RuleContextExecutionSupport;

/**
 * Runtime service for executing compiled or ad hoc DRL rules.
 *
 * <p>The service resolves rules by identifier, compiles caller-provided DRL
 * when requested, and updates the supplied {@link RuleContext} with execution
 * results. Every execution requires a context with a non-blank method, and that
 * method must match exactly one rule.
 */
public interface DRLService {

    /**
     * Resolves, compiles, caches, and executes the rule identified by the given
     * rule id.
     *
     * <p>The runtime fails if the context has no method, no rule fires, or more
     * than one rule fires for the requested method.
     *
     * @param ruleId the rule identifier used by the configured source resolver
     * @param context the execution context and fact carrier
     */
    <T> void execute(String ruleId, RuleContext<T> context);

    /**
     * Invokes the target in {@code ruleId#method} form and returns the rule output.
     *
     * <p>The rule id is passed to the configured source resolver through
     * {@link #execute(String, RuleContext)}. Facts and parameters are copied into
     * a fresh context. Execution failures propagate to the caller.
     *
     * @param target the stored rule id and context method separated by a single
     *        {@code #}, with surrounding whitespace ignored
     * @param facts the session facts, or {@code null} for no facts
     * @param params the named parameters, or {@code null} for no parameters
     * @return the rule output, which may be {@code null}
     * @throws IllegalArgumentException if the target does not contain two
     *         non-blank parts separated by a single {@code #}
     */
    default <T> T invoke(String target, List<?> facts, Map<String, ?> params) {
        int separator = target == null ? -1 : target.indexOf('#');
        if (separator < 0 || separator != target.lastIndexOf('#')) {
            throw new IllegalArgumentException("DRL target must use ruleId#method with a single '#'.");
        }

        String ruleId = target.substring(0, separator).strip();
        String method = target.substring(separator + 1).strip();
        if (ruleId.isEmpty() || method.isEmpty()) {
            throw new IllegalArgumentException("DRL target requires a rule id and method in ruleId#method form.");
        }

        RuleContext<T> context = RuleContext.method(method);
        if (facts != null) {
            facts.forEach(context::fact);
        }
        if (params != null) {
            context.getParams().putAll(params);
        }

        execute(ruleId, context);
        return RuleContextExecutionSupport.outputOrThrow(context, "DRL target '" + target + "'");
    }

    /**
     * Compiles the supplied DRL source, stores the compiled rule base under the
     * given rule id, and executes it immediately.
     *
     * <p>The runtime fails if the context has no method, no rule fires, or more
     * than one rule fires for the requested method.
     *
     * @param ruleId the cache key to use for the compiled rule base
     * @param source the DRL source text to compile
     * @param context the execution context and fact carrier
     */
    <T> void execute(String ruleId, String source, RuleContext<T> context);

    /**
     * Compiles and executes the supplied DRL source without consulting a source
     * resolver or storing the compiled result in the cache.
     *
     * <p>The runtime fails if the context has no method, no rule fires, or more
     * than one rule fires for the requested method.
     *
     * @param source the DRL source text to compile
     * @param context the execution context and fact carrier
     */
    <T> void executeSource(String source, RuleContext<T> context);

    /**
     * Clears all cached compiled rule bases.
     */
    void purgeCache();

    /**
     * Removes a single cached compiled rule base.
     *
     * @param ruleId the cache key to remove
     */
    void purgeCache(String ruleId);

    /**
     * Checks whether a compiled rule base is currently cached for the rule id.
     *
     * @param ruleId the cache key to check
     * @return {@code true} if a compiled rule base is present in the cache
     */
    boolean isCached(String ruleId);
    
    /**
     * Resolves the DRL source for the supplied rule id, compiles it, and stores
     * the compiled rule base in the runtime cache without executing it.
     *
     * <p>Use this method to warm the cache during application startup, after a
     * rule has been added to an external store, or after a previously cached
     * rule has been purged. Later calls to {@link #execute(String, RuleContext)}
     * can reuse the cached compiled form instead of resolving and compiling the
     * same rule on first execution.
     *
     * @param ruleId the rule identifier to resolve through the configured source
     *        resolver and use as the cache key
     */
    void compileAndCache(String ruleId);
    
    /**
     * Compiles the supplied DRL source and stores the compiled rule base in the
     * runtime cache under the supplied rule id without executing it.
     *
     * <p>Use this method when the caller already has the DRL source text, for
     * example after reading a rule from a database, object storage, or a
     * developer tooling endpoint. The source resolver is not consulted.
     *
     * @param ruleId the cache key to associate with the compiled rule base and
     *        the label used in compilation error messages
     * @param source the DRL source text to compile and cache
     */
    void compileAndCache(String ruleId, String source);
    
    /**
     * Resolves the DRL source for the supplied rule id and compiles it into an
     * executable rule bundle without storing it in the runtime cache or firing
     * any rules.
     *
     * <p>Use this method when a caller needs to validate or inspect a rule
     * before deciding whether it should be cached or executed. Later execution
     * is the caller's responsibility unless the compiled result is passed into a
     * cache-aware flow.
     *
     * @param ruleId the rule identifier to resolve through the configured source
     *        resolver and use in compilation error messages
     * @return the compiled rule bundle, including the KIE base and detected
     *         runtime metadata
     */
    CompiledDRLRule compile(String ruleId);
    
    /**
     * Compiles the supplied DRL source into an executable rule bundle without
     * consulting the source resolver, storing it in the runtime cache, or firing
     * any rules.
     *
     * <p>Use this method for compile-only validation of caller-provided source,
     * such as source read from a database, file, object storage, or generated by
     * developer tooling.
     *
     * @param ruleId the label used in compilation error messages
     * @param drl the DRL source text to compile
     * @return the compiled rule bundle, including the KIE base and detected
     *         runtime metadata
     */
    CompiledDRLRule compile(String ruleId, String drl);
}
