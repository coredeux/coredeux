package com.coredeux.demo.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.context.OperationContext;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.validation.ValidationError;
import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.CustomerStatus;

class CustomerEmailValidatorTest {

    @Test
    void validatesStatusAndHandlesNullCustomers() {
        CustomerEmailValidator validator = new CustomerEmailValidator();
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName(Customer.class.getName())
                .name("customer")
                .identifier("pk")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("customerDataAccess.drl").build())
                .build();
        OperationContext context = OperationContext.builder().build();

        List<ValidationError> nullErrors = validator.validate(null, definition, context);
        assertEquals(1, nullErrors.size());
        assertEquals("entity", nullErrors.get(0).getField());

        Customer missingStatus = new Customer();
        missingStatus.setName("Alice");
        missingStatus.setEmail("alice@example.com");
        missingStatus.setActive(true);
        List<ValidationError> errors = validator.validate(missingStatus, definition, context);
        assertEquals(1, errors.size());
        assertTrue(errors.stream().anyMatch(error -> "status".equals(error.getField())));

        missingStatus.setStatus(CustomerStatus.ACTIVE);
        assertTrue(validator.validate(missingStatus, definition, context).isEmpty());
    }
}
