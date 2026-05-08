package com.coredeux.core.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ValidationErrorCoverageTest {

    @Test
    void shouldExerciseNoArgsConstructorAndSetters() {
        ValidationError error = new ValidationError();

        assertNull(error.getField());
        assertNull(error.getMessage());

        error.setField("name");
        error.setMessage("required");

        assertEquals("name", error.getField());
        assertEquals("required", error.getMessage());
        assertNotNull(error.toString());
    }

    @Test
    void shouldExerciseAllArgsConstructorAndBuilderEquality() {
        ValidationError constructed = new ValidationError("code", "invalid");
        ValidationError built = ValidationError.builder()
                .field("code")
                .message("invalid")
                .build();
        ValidationError different = ValidationError.builder()
                .field("name")
                .message("required")
                .build();

        assertEquals("code", constructed.getField());
        assertEquals("invalid", constructed.getMessage());
        assertEquals(constructed, built);
        assertEquals(constructed.hashCode(), built.hashCode());
        assertNotEquals(constructed, different);
    }
}
