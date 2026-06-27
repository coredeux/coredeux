package com.coredeux.drl.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.drl.cache.CompiledDRLRule;
import com.coredeux.drl.cache.DRLCache;
import com.coredeux.drl.cache.InMemoryDRLCache;
import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;
import com.coredeux.drl.source.resolver.DRLSourceResolver;
import com.coredeux.drl.support.RuleContextExecutionSupport;

/**
 * Default runtime implementation of {@link DRLService}.
 *
 * <p>This implementation resolves DRL source, compiles it into a {@link KieBase},
 * caches compiled rule bases by rule identifier, and executes rules against a
 * fresh {@link KieSession} per invocation.
 */
public class DefaultDRLService implements DRLService {

    private static final String COMPONENT_REGISTRY = "componentRegistry";

    static {
        DrlRuntimeBootstrap.initialize();
    }

    private final DRLSourceResolver sourceResolver;
    private final CoredeuxComponentRegistry componentRegistry;
    private final DRLCache cache;

    /**
     * Creates a runtime service backed by the default in-memory component
     * registry and in-memory rule cache.
     *
     * @param sourceResolver resolver used to load DRL by rule id
     */
    public DefaultDRLService(DRLSourceResolver sourceResolver) {
        this(sourceResolver, new InMemoryCoredeuxComponentRegistry(Map.of()));
    }

    /**
     * Creates a runtime service backed by the supplied component registry and a
     * default in-memory rule cache.
     *
     * @param sourceResolver resolver used to load DRL by rule id
     * @param componentRegistry registry exposed to rules through the
     *        {@code componentRegistry} global
     */
    public DefaultDRLService(DRLSourceResolver sourceResolver, CoredeuxComponentRegistry componentRegistry) {
        this(sourceResolver, componentRegistry, new InMemoryDRLCache());
    }

    /**
     * Creates a runtime service with fully supplied dependencies.
     *
     * @param sourceResolver resolver used to load DRL by rule id
     * @param componentRegistry registry exposed to rules through the
     *        {@code componentRegistry} global
     * @param cache cache used to store compiled rule bases by rule id
     */
    public DefaultDRLService(DRLSourceResolver sourceResolver, CoredeuxComponentRegistry componentRegistry,
            DRLCache cache) {
        this.sourceResolver = Objects.requireNonNull(sourceResolver, "sourceResolver");
        this.componentRegistry = Objects.requireNonNull(componentRegistry, COMPONENT_REGISTRY);
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    /**
     * Executes the rule identified by {@code ruleId}, compiling and caching it
     * on first use.
     *
     * @param ruleId the rule identifier used by the configured source resolver
     * @param context the execution context and fact carrier
     */
    @Override
    public <T> void execute(String ruleId, RuleContext<T> context) {
        CompiledDRLRule compiledRule = cache.computeIfAbsent(ruleId, () -> compile(ruleId));
        executeCompiled(compiledRule, context);
    }

    /**
     * Compiles the supplied DRL source, caches it under {@code ruleId}, and
     * executes it immediately.
     *
     * @param ruleId the cache key to use for the compiled rule base
     * @param source the DRL source text to compile
     * @param context the execution context and fact carrier
     */
    @Override
    public <T> void execute(String ruleId, String source, RuleContext<T> context) {
        CompiledDRLRule compiledRule = compile(ruleId, source);
        cache.put(ruleId, compiledRule);
        executeCompiled(compiledRule, context);
    }

    /**
     * Compiles and executes the supplied DRL source without caching it.
     *
     * @param source the DRL source text to compile
     * @param context the execution context and fact carrier
     */
    @Override
    public <T> void executeSource(String source, RuleContext<T> context) {
        executeCompiled(compile("inline source", source), context);
    }

    /**
     * Removes all cached compiled rule bases.
     */
    @Override
    public void purgeCache() {
        cache.clear();
    }

    /**
     * Removes a single cached compiled rule base.
     *
     * @param ruleId the cache key to remove
     */
    @Override
    public void purgeCache(String ruleId) {
        cache.remove(ruleId);
    }

    /**
     * Reports whether the given rule id currently has a cached compiled base.
     *
     * @param ruleId the cache key to check
     * @return {@code true} if a compiled rule base is present in the cache
     */
    @Override
    public boolean isCached(String ruleId) {
        return cache.contains(ruleId);
    }
    

    /**
     * Resolves the rule source through the configured resolver, compiles it, and
     * stores the compiled rule bundle in the cache without firing any rules.
     *
     * @param ruleId the rule identifier to resolve and cache
     */
	@Override
	public void compileAndCache(String ruleId) {
		CompiledDRLRule compiledRule = compile(ruleId);
		cache.put(ruleId, compiledRule);
	}

    /**
     * Compiles the provided source text and stores the compiled rule bundle in
     * the cache under the supplied rule id without firing any rules.
     *
     * @param ruleId the cache key and compilation label for the source
     * @param source the DRL source text to compile and cache
     */
	@Override
	public void compileAndCache(String ruleId, String source) {
		CompiledDRLRule compiledRule = compile(ruleId, source);
		cache.put(ruleId, compiledRule);
	}

    /**
     * Resolves DRL through the configured source resolver and compiles it into a
     * rule bundle without caching or executing it.
     *
     * @param ruleId the rule identifier to resolve and use in compilation errors
     * @return the compiled rule bundle, including KIE base metadata
     */
	@Override
	public CompiledDRLRule compile(String ruleId) {
        return compile(ruleId, sourceResolver.resolve(ruleId));
    }

    /**
     * Compiles the supplied source text into a rule bundle without caching or
     * executing it.
     *
     * @param ruleId label used in any compilation error messages
     * @param drl the DRL source text to compile
     * @return the compiled rule bundle, including KIE base metadata
     */
	@Override
	public CompiledDRLRule compile(String ruleId, String drl) {
        KieHelper helper = new KieHelper();
        helper.addContent(requireSource(drl, ruleId), ResourceType.DRL);

        Results results = helper.verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            String errors = results.getMessages(Message.Level.ERROR).stream()
                    .map(Message::toString)
                    .collect(Collectors.joining(System.lineSeparator()));
            throw new IllegalStateException("DRL failed to compile for ruleId '" + ruleId + "':"
                    + System.lineSeparator() + errors);
        }

        KieBase kieBase = helper.build();
        return new CompiledDRLRule(kieBase, requiresComponentRegistry(kieBase));
    }

    /**
     * Executes the compiled rule bundle against a new session and updates the
     * supplied context with the outcome.
     *
     * @param compiledRule the compiled rule bundle
     * @param context the execution context to insert and update
     */
    private void executeCompiled(CompiledDRLRule compiledRule, RuleContext<?> context) {
        try (KieSession session = compiledRule.kieBase().newKieSession()) {
            if (compiledRule.requiresComponentRegistry()) {
                session.setGlobal(COMPONENT_REGISTRY, componentRegistry);
            }
            session.insert(context);
            List<Object> facts = context.getFacts();
            if (facts != null) {
                for (Object fact : facts) {
                    if (fact != null) {
                        session.insert(fact);
                    }
                }
            }
            context.setFiredRules(session.fireAllRules());
            RuleContextExecutionSupport.throwIfException(context, "DRL execution");
        }
    }

    /**
     * Checks whether the compiled KieBase declares the component registry
     * global.
     *
     * @param kieBase the compiled rule base
     * @return {@code true} when the global is declared
     */
    private boolean requiresComponentRegistry(KieBase kieBase) {
        return kieBase.getKiePackages().stream()
                .flatMap(kiePackage -> kiePackage.getGlobalVariables().stream())
                .map(org.kie.api.definition.rule.Global::getName)
                .anyMatch(COMPONENT_REGISTRY::equals);
    }

    /**
     * Validates that the supplied DRL source is present before compilation.
     *
     * @param source the source text to validate
     * @param sourceLabel label used in any error message
     * @return the validated source text
     */
    private String requireSource(String source, String sourceLabel) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("DRL source is required for " + sourceLabel);
        }
        return source;
    }
}
