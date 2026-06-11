package com.coredeux.demo.drl.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.coredeux.demo.domain.Customer;
import com.coredeux.drl.model.RuleContext;

class CustomerDrlAuditRuleSourceTest {

    @Test
    void setsTheAuditMessageForTheCurrentPhase() {
        CustomerDrlAuditRuleSource source = new CustomerDrlAuditRuleSource();

        RuleContext<Customer> context = RuleContext.<Customer>method("audit").param("phase", "UPSERT");
        source.audit(context);

        assertEquals("DRL: Audited UPSERT", context.getMessage());
    }
}
