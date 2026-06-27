package com.coredeux.drl.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxCoreException;

class CoredeuxDRLExceptionTest {

    @Test
    void shouldSupportAllConstructors() throws Exception {
        CoredeuxDRLException noArgs = newInstance();
        CoredeuxDRLException messageOnly = new CoredeuxDRLException("boom");
        RuntimeException cause = new RuntimeException("cause");
        CoredeuxDRLException messageAndCause = new CoredeuxDRLException("boom", cause);
        CoredeuxDRLException causeOnly = new CoredeuxDRLException(cause);

        assertInstanceOf(CoredeuxCoreException.class, noArgs);
        assertNull(noArgs.getMessage());

        assertEquals("boom", messageOnly.getMessage());

        assertEquals("boom", messageAndCause.getMessage());
        assertSame(cause, messageAndCause.getCause());

        assertSame(cause, causeOnly.getCause());
    }

    private CoredeuxDRLException newInstance() throws Exception {
        Constructor<CoredeuxDRLException> constructor = CoredeuxDRLException.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
