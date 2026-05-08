package com.coredeux.impex.handler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.handler.ImportValueContext;
import com.coredeux.impex.model.ImportColumn;

class JsonMapImportHandlerTest {

    private final JsonMapImportHandler handler = new JsonMapImportHandler();

    @Test
    void shouldConvertMapValuesToTemporalNumericAndUuidTypes() {
        assertEquals(Map.of("value", true), handle(Map.of("value", "true"), boolean.class));
        assertEquals(Map.of("value", 7), handle(Map.of("value", "7"), int.class));
        assertEquals(Map.of("value", 8L), handle(Map.of("value", "8"), long.class));
        assertEquals(Map.of("value", (short) 9), handle(Map.of("value", "9"), short.class));
        assertEquals(Map.of("value", (byte) 10), handle(Map.of("value", "10"), byte.class));
        assertEquals(Map.of("value", 1.5d), handle(Map.of("value", "1.5"), double.class));
        assertEquals(Map.of("value", 2.5f), handle(Map.of("value", "2.5"), float.class));
        assertEquals(Map.of("value", new BigInteger("123")), handle(Map.of("value", "123"), BigInteger.class));
        assertEquals(Map.of("value", new BigDecimal("12.30")), handle(Map.of("value", "12.30"), BigDecimal.class));
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertEquals(Map.of("value", uuid), handle(Map.of("value", uuid.toString()), UUID.class));
        assertEquals(Map.of("value", LocalDate.of(2026, 5, 8)), handle(Map.of("value", "2026-05-08"), LocalDate.class));
        assertEquals(Map.of("value", LocalDateTime.of(2026, 5, 8, 10, 15)),
                handle(Map.of("value", "2026-05-08T10:15:00"), LocalDateTime.class));
        assertEquals(Map.of("value", Instant.parse("2026-05-08T00:00:00Z")),
                handle(Map.of("value", "2026-05-08T00:00:00Z"), Instant.class));
        assertEquals(Map.of("value", OffsetDateTime.parse("2026-05-08T10:15:00+10:00")),
                handle(Map.of("value", "2026-05-08T10:15:00+10:00"), OffsetDateTime.class));
        assertEquals(Map.of("value", SampleStatus.ACTIVE), handle(Map.of("value", "ACTIVE"), SampleStatus.class));
    }

    @Test
    void shouldRejectCharacterMapValueType() {
        CoredeuxImportException exception = assertThrows(CoredeuxImportException.class,
                () -> handle(Map.of("value", "A"), char.class));

        assertTrue(exception.getMessage().contains("No JSON map conversion available"));
    }

    @Test
    void shouldPreserveObjectMapScalarValuesAndNulls() {
        Map<Object, Object> raw = new LinkedHashMap<>();
        raw.put("name", "demo");
        raw.put("count", 7);
        raw.put("enabled", true);
        raw.put("letter", 'A');
        raw.put("empty", null);

        Object converted = handle(raw, Object.class);

        assertEquals(raw, converted);
    }

    @Test
    void shouldReturnNullForNullRawValue() {
        Object converted = handler.handle(context(null, Object.class));

        assertNull(converted);
    }

    @Test
    void shouldHandleMissingColumnContextInErrors() {
        CoredeuxImportException exception = assertThrows(CoredeuxImportException.class,
                () -> handler.handle(ImportValueContext.builder()
                        .rawValue(42)
                        .mapValueType(Object.class)
                        .build()));

        assertNull(exception.getColumn());
        assertTrue(exception.getMessage().contains("for column: null"));
    }

    @Test
    void shouldRejectUnsupportedRawAndScalarTypesWithColumnContext() {
        CoredeuxImportException unsupportedRaw = assertThrows(CoredeuxImportException.class,
                () -> handler.handle(context(42, Object.class)));
        CoredeuxImportException unsupportedScalar = assertThrows(CoredeuxImportException.class,
                () -> handle(Map.of("value", "x"), Field.class));
        CoredeuxImportException badNumber = assertThrows(CoredeuxImportException.class,
                () -> handle(Map.of("value", "not-a-number"), Integer.class));

        assertEquals("attributes", unsupportedRaw.getColumn());
        assertTrue(unsupportedRaw.getMessage().contains("Unsupported map import value type"));
        assertEquals("attributes", unsupportedScalar.getColumn());
        assertTrue(unsupportedScalar.getMessage().contains("No JSON map conversion available"));
        assertEquals("attributes", badNumber.getColumn());
        assertTrue(badNumber.getMessage().contains("Unable to convert map value"));
    }

    @Test
    void shouldResolveMapValueType() throws Exception {
        assertEquals(Object.class, JsonMapImportHandler.mapValueType(null));
        assertEquals(Object.class, JsonMapImportHandler.mapValueType(SampleFields.class.getDeclaredField("notMap")));
        assertEquals(Object.class, JsonMapImportHandler.mapValueType(SampleFields.class.getDeclaredField("rawMap")));
        assertEquals(Integer.class, JsonMapImportHandler.mapValueType(SampleFields.class.getDeclaredField("scores")));
    }

    private Object handle(Object raw, Class<?> mapValueType) {
        return handler.handle(context(raw, mapValueType));
    }

    private ImportValueContext context(Object raw, Class<?> mapValueType) {
        return ImportValueContext.builder()
                .rawValue(raw)
                .mapValueType(mapValueType)
                .column(ImportColumn.builder().name("attributes").build())
                .build();
    }

    private enum SampleStatus {
        ACTIVE
    }

    @SuppressWarnings("rawtypes")
    private static class SampleFields {

        private String notMap;
        private Map rawMap;
        private Map<String, Integer> scores;
    }
}
