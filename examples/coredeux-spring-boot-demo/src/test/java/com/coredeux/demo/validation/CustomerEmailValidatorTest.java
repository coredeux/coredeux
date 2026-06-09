package com.coredeux.demo.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.validation.ValidationError;
import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.CustomerStatus;

class CustomerEmailValidatorTest {

    private final CustomerEmailValidator validator = new CustomerEmailValidator();

    @Test
    void shouldReturnNoErrorsForValidCustomer() {
        List<ValidationError> errors = validator.validate(customer("Alice", "alice@example.com", CustomerStatus.ACTIVE),
                definition(), OperationContext.builder().build());

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldReportMissingFields() {
        List<ValidationError> errors = validator.validate(customer(" ", "invalid", null), definition(),
                OperationContext.builder().build());

        assertEquals(3, errors.size());
        assertTrue(errors.stream().anyMatch(error -> "name".equals(error.getField())));
        assertTrue(errors.stream().anyMatch(error -> "email".equals(error.getField())));
        assertTrue(errors.stream().anyMatch(error -> "status".equals(error.getField())));
    }

    private Customer customer(String name, String email, CustomerStatus status) {
        return Customer.builder().name(name).email(email).status(status).build();
    }

    private CoredeuxEntityDefinition definition() {
        return CoredeuxEntityDefinition.builder()
                .name("customer")
                .fullClassName(Customer.class.getName())
                .identifier("pk")
                .build();
    }
}
