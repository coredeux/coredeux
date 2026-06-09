package com.coredeux.demo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;

import org.junit.jupiter.api.Test;

class UriAttributeConverterTest {

    private final UriAttributeConverter converter = new UriAttributeConverter();

    @Test
    void shouldConvertUriToDatabaseAndBack() {
        URI uri = URI.create("https://coredeux.example/demo");

        assertEquals("https://coredeux.example/demo", converter.convertToDatabaseColumn(uri));
        assertEquals(uri, converter.convertToEntityAttribute("https://coredeux.example/demo"));
    }

    @Test
    void shouldHandleNullAndBlankValues() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }
}
