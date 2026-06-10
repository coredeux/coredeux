package com.coredeux.demo.drl.validation;

import java.util.ArrayList;
import java.util.List;

import com.coredeux.core.registry.CoredeuxComponentRegistry;
import com.coredeux.core.validation.ValidationError;
import com.coredeux.demo.domain.Customer;
import com.coredeux.drl.converter.annotations.DrlDefinition;
import com.coredeux.drl.converter.annotations.DrlGlobal;
import com.coredeux.drl.converter.annotations.DrlRule;
import com.coredeux.drl.model.RuleContext;

@DrlDefinition("customerValidator.drl")
public class CustomerDrlValidatorRuleSource {

    @DrlGlobal
    public CoredeuxComponentRegistry componentRegistry;

    @DrlRule(name = "validate", when = "$context : RuleContext(method == 'validate')")
    public void validate(RuleContext<List<ValidationError>> $context) {
        Customer customer = (Customer) $context.getParams().get("entity");
        List<ValidationError> errors = new ArrayList<>();
        if (customer == null) {
            errors.add(ValidationError.builder().field("entity").message("Customer is required").build());
        } else {
            if (customer.getName() == null || customer.getName().isBlank()) {
                errors.add(ValidationError.builder().field("name").message("Customer name is required").build());
            }
            if (customer.getEmail() == null || !customer.getEmail().contains("@")) {
                errors.add(ValidationError.builder().field("email").message("Customer email must be valid").build());
            }
        }
        $context.setOutput(errors);
    }
}
