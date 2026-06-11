package com.coredeux.demo.drl.audit;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.demo.domain.Customer;
import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlGlobal;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.model.RuleContext;

@DrlDefinition("customerAudit.drl")
public class CustomerDrlAuditRuleSource {

    @DrlGlobal
    public CoredeuxComponentRegistry componentRegistry;

    @DrlRule(name = "audit", when = "$context : RuleContext(method == 'audit')")
    public void audit(RuleContext<Customer> $context) {
    	System.out.println("Auditing customer with DRL: " + $context.getParams().get("phase"));
        $context.setMessage("DRL: Audited " + $context.getParams().get("phase"));
    }
}
