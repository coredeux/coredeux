package com.coredeux.impex.handler.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.coredeux.impex.exception.CoredeuxImportException;
import com.coredeux.impex.handler.ImportValueContext;
import com.coredeux.impex.model.ImportColumn;

class DefaultCoredeuxImportValueHandlerTest {

    private final DefaultCoredeuxImportValueHandler handler = new DefaultCoredeuxImportValueHandler(null);

    @Test
    void shouldConvertScalarTypes() {
        assertEquals(" raw ", handle(" raw ", String.class));
        assertEquals(7, handle("7", int.class));
        assertEquals(8L, handle("8", long.class));
        assertEquals((short) 9, handle("9", short.class));
        assertEquals((byte) 10, handle("10", byte.class));
        assertEquals(1.5d, handle("1.5", double.class));
        assertEquals(2.5f, handle("2.5", float.class));
        assertEquals(new BigDecimal("3.50"), handle("3.50", BigDecimal.class));
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertEquals(uuid, handle(uuid.toString(), UUID.class));
        assertEquals(LocalDate.of(2026, 5, 8), handle("2026-05-08", LocalDate.class));
        assertEquals(LocalDateTime.of(2026, 5, 8, 10, 15), handle("2026-05-08T10:15:00", LocalDateTime.class));
        assertEquals(Instant.parse("2026-05-08T00:00:00Z"), handle("2026-05-08T00:00:00Z", Instant.class));
        assertEquals(OffsetDateTime.parse("2026-05-08T10:15:00+10:00"),
                handle("2026-05-08T10:15:00+10:00", OffsetDateTime.class));
        assertEquals(SampleStatus.ACTIVE, handle("ACTIVE", SampleStatus.class));
    }

    @Test
    void shouldReturnPrimitiveDefaultsForBlankValues() {
        assertEquals(false, handle(" ", boolean.class));
        assertEquals('\0', handle(" ", char.class));
        assertEquals(0, handle(" ", int.class));
        assertNull(handle(" ", Integer.class));
    }

    @Test
    void shouldReturnRawValueWhenExpectedTypeIsObjectOrMissing() {
        assertEquals("raw", handle("raw", Object.class));
        Object missingType = handler.handle(ImportValueContext.builder()
                .rawValue("raw")
                .effectiveValue("raw")
                .column(ImportColumn.builder().name("sample").build())
                .build());

        assertEquals("raw", missingType);
    }

    @Test
    void shouldConvertCollectionsAndPreserveEscapedSeparators() {
        Object list = handler.handle(context("one,two\\,too,three\\\\", List.class)
                .collectionElementType(String.class)
                .build());
        Object set = handler.handle(context("1,2,2", Set.class)
                .collectionElementType(Integer.class)
                .build());

        assertEquals(List.of("one", "two,too", "three\\"), list);
        assertEquals(new LinkedHashSet<>(List.of(1, 2)), set);
    }

    @Test
    void shouldRejectUnsupportedConversion() {
        CoredeuxImportException exception = assertThrows(CoredeuxImportException.class,
                () -> handle("value", Field.class));
        CoredeuxImportException charException = assertThrows(CoredeuxImportException.class,
                () -> handle("A", char.class));

        assertTrue(exception.getMessage().contains("No default import conversion available"));
        assertTrue(exception.getMessage().contains("sample"));
        assertTrue(charException.getMessage().contains("No default import conversion available"));
    }

    @Test
    void shouldResolveCollectionElementType() throws Exception {
        assertEquals(String.class, DefaultCoredeuxImportValueHandler.collectionElementType(null));
        assertEquals(String.class,
                DefaultCoredeuxImportValueHandler.collectionElementType(SampleFields.class.getDeclaredField("rawList")));
        assertEquals(Integer.class,
                DefaultCoredeuxImportValueHandler.collectionElementType(SampleFields.class.getDeclaredField("numbers")));
    }

    private Object handle(String value, Class<?> expectedType) {
        return handler.handle(context(value, expectedType).build());
    }

    private ImportValueContext.ImportValueContextBuilder context(String value, Class<?> expectedType) {
        return ImportValueContext.builder()
                .rawValue(value)
                .effectiveValue(value)
                .expectedType(expectedType)
                .column(ImportColumn.builder().name("sample").build());
    }

    private enum SampleStatus {
        ACTIVE
    }

    @SuppressWarnings("rawtypes")
    private static class SampleFields {

        private List rawList;
        private List<Integer> numbers;
    }
}
