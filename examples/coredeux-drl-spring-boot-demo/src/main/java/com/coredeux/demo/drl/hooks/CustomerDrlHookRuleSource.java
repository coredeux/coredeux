package com.coredeux.demo.drl.hooks;

import java.time.Instant;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.demo.domain.Customer;
import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlGlobal;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.model.RuleContext;

@DrlDefinition("customerHook.drl")
public class CustomerDrlHookRuleSource {

    @DrlGlobal
    public CoredeuxComponentRegistry componentRegistry;

    @DrlRule(name = "beforeSave", when = "$context : RuleContext(method == 'beforeSave')")
    public void beforeSave(RuleContext<Customer> $context) {
        Object entity = $context.getParams().get("entity");
        if (entity instanceof Customer) {
            ((Customer) entity).setLastLifecycleTouch(Instant.now());
        }
    }

    @DrlRule(name = "beforeUpdate", when = "$context : RuleContext(method == 'beforeUpdate')")
    public void beforeUpdate(RuleContext<Customer> $context) {
        Object entity = $context.getParams().get("entity");
        if (entity instanceof Customer) {
            ((Customer) entity).setLastLifecycleTouch(Instant.now());
        }
    }
}
