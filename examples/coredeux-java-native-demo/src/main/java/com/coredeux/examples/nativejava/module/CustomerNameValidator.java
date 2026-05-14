package com.coredeux.examples.nativejava.module;

import java.util.List;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.validation.CoredeuxEntityValidator;
import com.coredeux.core.validation.ValidationError;
import com.coredeux.demo.domain.Customer;

public class CustomerNameValidator implements CoredeuxEntityValidator<Customer> {

    @Override
    public List<ValidationError> validate(Customer entity, CoredeuxEntityDefinition definition,
            OperationContext context) {
        if (entity.getName() == null || entity.getName().isBlank()) {
            return List.of(ValidationError.builder()
                    .field("name")
                    .message("Customer name is required")
                    .build());
        }
        return List.of();
    }
}
