package com.coredeux.drl.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class DrlConversionExceptionTest {

    @Test
    void storesMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("boom");
        DrlConversionException exception = new DrlConversionException("failed", cause);

        assertEquals("failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
