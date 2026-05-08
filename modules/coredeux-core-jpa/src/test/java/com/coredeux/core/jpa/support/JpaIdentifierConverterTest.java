package com.coredeux.core.jpa.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxValidationException;

class JpaIdentifierConverterTest {

    @Test
    void shouldConvertSupportedIdentifierTypes() {
        UUID uuid = UUID.randomUUID();

        assertEquals("42", JpaIdentifierConverter.convert("42", String.class));
        assertEquals(42L, JpaIdentifierConverter.convert("42", Long.class));
        assertEquals(7, JpaIdentifierConverter.convert("7", Integer.class));
        assertEquals((short) 5, JpaIdentifierConverter.convert("5", Short.class));
        assertEquals((byte) 3, JpaIdentifierConverter.convert("3", Byte.class));
        assertEquals(4.5d, JpaIdentifierConverter.convert("4.5", Double.class));
        assertEquals(2.5f, JpaIdentifierConverter.convert("2.5", Float.class));
        assertEquals(new BigInteger("99"), JpaIdentifierConverter.convert("99", BigInteger.class));
        assertEquals(new BigDecimal("123.45"), JpaIdentifierConverter.convert("123.45", BigDecimal.class));
        assertEquals(uuid, JpaIdentifierConverter.convert(uuid.toString(), UUID.class));
        assertEquals(SampleEnum.ALPHA, JpaIdentifierConverter.convert("ALPHA", SampleEnum.class));
        assertEquals(CustomValueType.valueOf("custom"), JpaIdentifierConverter.convert("custom", CustomValueType.class));
        assertEquals(new ConstructorType("ctor"), JpaIdentifierConverter.convert("ctor", ConstructorType.class));
    }

    @Test
    void shouldReturnNullForNullIdentifier() {
        assertEquals(null, JpaIdentifierConverter.convert(null, String.class));
    }

    @Test
    void shouldFailForNullTargetTypeOrUnsupportedType() {
        assertThrows(CoredeuxValidationException.class, () -> JpaIdentifierConverter.convert("1", null));
        assertThrows(CoredeuxValidationException.class, () -> JpaIdentifierConverter.convert("1", UnsupportedType.class));
    }

    private enum SampleEnum {
        ALPHA
    }

    private static final class CustomValueType {

        private final String value;

        private CustomValueType(String value) {
            this.value = value;
        }

        public static CustomValueType valueOf(String value) {
            return new CustomValueType(value);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CustomValueType that && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    private static final class ConstructorType {

        private final String value;

        public ConstructorType(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ConstructorType that && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    private static final class UnsupportedType {
    }
}
