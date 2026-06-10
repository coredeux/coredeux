package com.coredeux.demo.validation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.validation.CoredeuxEntityValidator;
import com.coredeux.core.validation.ValidationError;
import com.coredeux.demo.domain.Customer;

@Component("customerEmailValidator")
public class CustomerEmailValidator implements CoredeuxEntityValidator<Customer> {

    @Override
    public List<ValidationError> validate(Customer customer, CoredeuxEntityDefinition definition,
            OperationContext context) {
        List<ValidationError> errors = new ArrayList<>();
        if (customer == null) {
            errors.add(ValidationError.builder().field("entity").message("Customer is required").build());
            return errors;
        }
        if (customer.getName() == null || customer.getName().isBlank()) {
            errors.add(ValidationError.builder().field("name").message("Customer name is required").build());
        }
        if (customer.getEmail() == null || !customer.getEmail().contains("@")) {
            errors.add(ValidationError.builder().field("email").message("Customer email must be valid").build());
        }
        if (customer.getStatus() == null) {
            errors.add(ValidationError.builder().field("status").message("Customer status is required").build());
        }
        return errors;
    }
}
