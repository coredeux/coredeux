package com.coredeux.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class CoredeuxGenericTypeResolverTest {

    @Test
    void shouldResolveGenericFromDirectInterface() {
        assertEquals(Customer.class,
                CoredeuxGenericTypeResolver.resolveFirstGeneric(CustomerHandler.class, Handler.class));
    }

    @Test
    void shouldResolveGenericFromParentClass() {
        assertEquals(Customer.class,
                CoredeuxGenericTypeResolver.resolveFirstGeneric(ConcreteCustomerHandler.class, Handler.class));
    }

    @Test
    void shouldResolveRawTypeFromParameterizedArgument() {
        assertEquals(List.class,
                CoredeuxGenericTypeResolver.resolveFirstGeneric(ListHandler.class, Handler.class));
    }

    @Test
    void shouldReturnNullWhenInputsOrGenericAreNotResolvable() {
        assertNull(CoredeuxGenericTypeResolver.resolveFirstGeneric(null, Handler.class));
        assertNull(CoredeuxGenericTypeResolver.resolveFirstGeneric(CustomerHandler.class, null));
        assertNull(CoredeuxGenericTypeResolver.resolveFirstGeneric(RawHandler.class, Handler.class));
        assertNull(CoredeuxGenericTypeResolver.resolveFirstGeneric(String.class, Handler.class));
    }

    interface Handler<T> {
    }

    static class Customer {
    }

    static class CustomerHandler implements Handler<Customer> {
    }

    abstract static class AbstractCustomerHandler implements Handler<Customer> {
    }

    static class ConcreteCustomerHandler extends AbstractCustomerHandler {
    }

    static class ListHandler implements Handler<List<Customer>> {
    }

    @SuppressWarnings("rawtypes")
    static class RawHandler implements Handler {
    }
}
