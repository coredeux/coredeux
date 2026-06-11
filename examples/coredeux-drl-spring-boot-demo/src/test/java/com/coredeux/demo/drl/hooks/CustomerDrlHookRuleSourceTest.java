package com.coredeux.demo.drl.hooks;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.CustomerStatus;
import com.coredeux.drl.model.RuleContext;

class CustomerDrlHookRuleSourceTest {

    @Test
    void updatesCustomerLifecycleTimestampInTheDrlHook() {
        CustomerDrlHookRuleSource source = new CustomerDrlHookRuleSource();
        Customer customer = new Customer();
        customer.setName("Alice");
        customer.setEmail("alice@example.com");
        customer.setActive(true);
        customer.setStatus(CustomerStatus.ACTIVE);

        RuleContext<Customer> beforeSave = RuleContext.<Customer>method("beforeSave").param("entity", customer);
        source.beforeSave(beforeSave);
        assertNotNull(customer.getLastLifecycleTouch());

        RuleContext<Customer> beforeUpdate = RuleContext.<Customer>method("beforeUpdate").param("entity", customer);
        source.beforeUpdate(beforeUpdate);
        assertNotNull(customer.getLastLifecycleTouch());
    }
}
