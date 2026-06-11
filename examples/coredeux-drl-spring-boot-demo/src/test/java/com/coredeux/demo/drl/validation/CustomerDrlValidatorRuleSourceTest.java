package com.coredeux.demo.drl.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.validation.ValidationError;
import com.coredeux.demo.domain.Customer;
import com.coredeux.drl.model.RuleContext;

class CustomerDrlValidatorRuleSourceTest {

    @Test
    void validatesCustomerNameAndEmailInTheDrlVersion() {
        CustomerDrlValidatorRuleSource source = new CustomerDrlValidatorRuleSource();

        RuleContext<List<ValidationError>> context = RuleContext.<List<ValidationError>>method("validate");
        source.validate(context);
        assertEquals(1, context.getOutput().size());
        assertEquals("entity", context.getOutput().get(0).getField());

        Customer customer = new Customer();
        customer.setName("");
        customer.setEmail("invalid");
        RuleContext<List<ValidationError>> customerContext = RuleContext.<List<ValidationError>>method("validate")
                .param("entity", customer);
        source.validate(customerContext);
        assertEquals(2, customerContext.getOutput().size());
        assertTrue(customerContext.getOutput().stream().anyMatch(error -> "name".equals(error.getField())));
        assertTrue(customerContext.getOutput().stream().anyMatch(error -> "email".equals(error.getField())));
    }
}
