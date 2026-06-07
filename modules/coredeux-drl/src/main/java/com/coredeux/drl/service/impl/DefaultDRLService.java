package com.coredeux.drl.service.impl;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.registry.InMemoryCoredeuxComponentRegistry;
import com.coredeux.drl.cache.DRLCache;
import com.coredeux.drl.cache.InMemoryDRLCache;
import com.coredeux.drl.config.DrlRuntimeBootstrap;
import com.coredeux.drl.model.RuleContext;
import com.coredeux.drl.service.DRLService;
import com.coredeux.drl.source.resolver.DRLSourceResolver;

import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

public class DefaultDRLService implements DRLService {

    static {
        DrlRuntimeBootstrap.initialize();
    }

    private final DRLSourceResolver sourceResolver;
    private final CoredeuxComponentRegistry componentRegistry;
    private final DRLCache cache;

    public DefaultDRLService(DRLSourceResolver sourceResolver) {
        this(sourceResolver, new InMemoryCoredeuxComponentRegistry(Map.of()));
    }

    public DefaultDRLService(DRLSourceResolver sourceResolver, CoredeuxComponentRegistry componentRegistry) {
        this(sourceResolver, componentRegistry, new InMemoryDRLCache());
    }

    public DefaultDRLService(DRLSourceResolver sourceResolver, CoredeuxComponentRegistry componentRegistry,
            DRLCache cache) {
        this.sourceResolver = Objects.requireNonNull(sourceResolver, "sourceResolver");
        this.componentRegistry = Objects.requireNonNull(componentRegistry, "componentRegistry");
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    @Override
    public void execute(String ruleId, RuleContext context) {
        execute(ruleId, context, new Object[0]);
    }

    @Override
    public void execute(String ruleId, RuleContext context, Object... facts) {
        KieBase kieBase = cache.computeIfAbsent(ruleId, () -> compile(ruleId));
        KieSession session = kieBase.newKieSession();
        try {
            session.setGlobal("componentRegistry", componentRegistry);
            session.insert(context);
            if (facts != null) {
                for (Object fact : facts) {
                    if (fact != null) {
                        session.insert(fact);
                    }
                }
            }
            context.setFiredRules(session.fireAllRules());
            if (context.getException() != null) {
                throw new IllegalStateException("Rule captured an exception", context.getException());
            }
        } finally {
            session.dispose();
        }
    }

    @Override
    public void purgeCache() {
        cache.clear();
    }

    @Override
    public void purgeCache(String ruleId) {
        cache.remove(ruleId);
    }

    @Override
    public boolean isCached(String ruleId) {
        return cache.contains(ruleId);
    }

    private KieBase compile(String ruleId) {
        String drl = sourceResolver.resolve(ruleId);
        KieHelper helper = new KieHelper();
        helper.addContent(drl, ResourceType.DRL);

        Results results = helper.verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            String errors = results.getMessages(Message.Level.ERROR).stream()
                    .map(Message::toString)
                    .collect(Collectors.joining(System.lineSeparator()));
            throw new IllegalStateException("DRL failed to compile for ruleId '" + ruleId + "':"
                    + System.lineSeparator() + errors);
        }

        return helper.build();
    }
}
