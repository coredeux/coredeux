package com.coredeux.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxDataAccessException;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxDataAccessService;

class AbstractCoredeuxDataAccessServiceTest {

    private final TestService service = new TestService();

    @Test
    void shouldValidateInputsAndBuildPaginationHelpers() {
        assertEquals(AbstractCoredeuxDataAccessService.SUPPORTED_COMPARATORS, service.supportedComparators(String.class));
        assertEquals("java.lang.String", service.typeName(String.class));
        assertEquals("null", service.typeName(null));
        assertTrue(service.isPagingEnabled(10, 1));
        assertFalse(service.isPagingEnabled(0, 1));
        assertFalse(service.isPagingEnabled(10, 0));

        assertEquals(List.of(), service.defaultResults(null));
        assertEquals(List.of(), service.defaultResults(List.of()));

        PaginationData pagination = service.buildPagination(25, 11, 5, 3);
        assertEquals(3L, pagination.getCurrentPage());
        assertEquals(5L, pagination.getPageSize());
        assertEquals(25L, pagination.getTotalResults());
        assertEquals(11L, pagination.getResultSize());
        assertEquals(3L, pagination.getTotalPages());

        PaginationData unpaged = service.buildPagination(25, 11, -1, -1);
        assertEquals(-1L, unpaged.getCurrentPage());
        assertNull(unpaged.getTotalResults());

        assertThrows(CoredeuxValidationException.class, () -> service.validateLoadInput(" ", String.class));
        assertThrows(CoredeuxValidationException.class, () -> service.validateLoadInput("1", null));
        assertThrows(CoredeuxValidationException.class, () -> service.validateQueryInput(" ", String.class));
        assertThrows(CoredeuxValidationException.class, () -> service.validateSearchType(null));
        assertThrows(CoredeuxValidationException.class, () -> service.validateEntity(null, "save"));
        assertEquals("demo", service.normalizeRequired(" demo ", "boom"));
        assertThrows(CoredeuxValidationException.class, () -> service.normalizeRequired(" ", "boom"));
    }

    @Test
    void shouldResolveIdentifiersAndReadWriteFields() throws Exception {
        AnnotatedEntity entity = new AnnotatedEntity();
        entity.id = "abc";
        assertEquals("abc", service.extractIdentifier(entity));
        assertEquals("abc", service.requireIdentifier(entity, "load"));

        service.setIdentifier(entity, 123L);
        assertEquals("123", entity.id);
        assertEquals("123", service.extractIdentifier(entity));

        PlainEntity plain = new PlainEntity();
        plain.id = "fallback";
        assertEquals("id", service.identifierField(PlainEntity.class).getName());
        assertEquals("id", service.findIdentifierField(PlainEntity.class).getName());
        assertEquals("id", service.findField(PlainEntity.class, "id").getName());
        assertNotNull(service.findAnnotatedField(AnnotatedEntity.class, Deprecated.class.getName()));
        assertTrue(service.hasAnnotation(AnnotatedEntity.class.getDeclaredField("value"), Deprecated.class.getName()));
        assertFalse(service.hasAnnotation(AnnotatedEntity.class.getDeclaredField("value"), Override.class.getName()));

        List<String> seen = new ArrayList<>();
        service.doWithFields(ChildEntity.class, field -> seen.add(field.getName()));
        assertTrue(seen.containsAll(List.of("childValue", "id", "parentValue")));

        ChildEntity source = new ChildEntity();
        source.childValue = "child";
        source.parentValue = 7;
        TargetEntity target = new TargetEntity();
        service.copyFields(source, target);
        assertEquals("child", target.childValue);
        assertEquals(7L, target.parentValue);
    }

    @Test
    void shouldConvertValuesAndIdentifiersAcrossSupportedTypes() {
        UUID uuid = UUID.randomUUID();

        assertEquals("1", service.convertIdentifier("1", String.class));
        assertEquals(1L, service.convertIdentifier("1", long.class));
        assertEquals(1, service.convertIdentifier("1", Integer.class));
        assertEquals((short) 1, service.convertIdentifier("1", Short.class));
        assertEquals((byte) 1, service.convertIdentifier("1", byte.class));
        assertEquals(new BigInteger("1"), service.convertIdentifier("1", BigInteger.class));
        assertEquals(new BigDecimal("1.25"), service.convertIdentifier("1.25", BigDecimal.class));
        assertEquals(Boolean.TRUE, service.convertIdentifier("true", boolean.class));
        assertEquals(uuid, service.convertIdentifier(uuid.toString(), UUID.class));
        assertEquals(DemoEnum.ONE, service.convertIdentifier("ONE", DemoEnum.class));
        assertEquals(DemoFactory.of("abc"), service.convertIdentifier("abc", DemoFactory.class));
        assertEquals(DemoFactory.valueOf("abc"), service.invokeStringFactory(DemoFactory.class, "abc"));
        assertEquals(DemoFromString.fromString("abc"), service.invokeStringFactory(DemoFromString.class, "abc"));
        assertEquals(DemoFactory.of("abc"), service.convertValue("abc", DemoFactory.class));
        assertEquals(4.5d, service.convertValue("4.5", double.class));
        assertEquals(4.5f, service.convertValue("4.5", float.class));
        assertEquals("true", service.convertValue(true, String.class));
        assertEquals("x", service.convertIdentifier("x", null));
        assertThrows(CoredeuxValidationException.class, () -> service.convertIdentifier("x", UnsupportedId.class));

        CoredeuxDataAccessException existing = new CoredeuxDataAccessException("x");
        assertSame(existing, service.wrap("message", existing));
        assertThrows(CoredeuxValidationException.class,
                () -> service.wrap("message", new CoredeuxValidationException("x")));
        assertNotNull(service.wrap("message", new IllegalStateException("boom")));
    }

    @Test
    void shouldSupportComparatorNormalizationAndIdentifierCaching() {
        assertEquals("EQUALS", service.normalizeComparatorsKey(" equals "));
        assertNull(service.normalizeComparatorsKey(null));
        assertSame(service.identifierField(PlainEntity.class), service.identifierField(PlainEntity.class));
    }

    private static final class TestService extends AbstractCoredeuxDataAccessService {

        @Override
        public <T> T load(String id, Class<T> type) {
            return null;
        }

        @Override
        public <T> String save(T entity) {
            return null;
        }

        @Override
        public <T> void update(T entity) {
        }

        @Override
        public <T> void remove(T entity) {
        }

        @Override
        public <T> SearchResult<T> loadAll(List<com.coredeux.core.search.SearchParams> params, Class<T> type,
                int pageSize, int currentPage) {
            return SearchResult.<T>builder().results(List.of()).build();
        }

        @Override
        public <T> SearchResult<T> query(String query, Map<String, Object> params, Class<T> type, int pageSize,
                int currentPage) {
            return SearchResult.<T>builder().results(List.of()).build();
        }

        @Override
        public <T> void refresh(T entity) {
        }
    }

    static class ParentEntity {
        long parentValue;
    }

    static class ChildEntity extends ParentEntity {
        String childValue;
        String id;
    }

    static class TargetEntity {
        String childValue;
        Long parentValue;
    }

    static class PlainEntity {
        String id;
    }

    static class AnnotatedEntity {
        @Deprecated
        String value;
        String id;
    }

    enum DemoEnum {
        ONE
    }

    record DemoFactory(String value) {
        public static DemoFactory valueOf(String value) {
            return new DemoFactory(value);
        }

        public static DemoFactory of(String value) {
            return new DemoFactory(value);
        }
    }

    record DemoFromString(String value) {
        public static DemoFromString fromString(String value) {
            return new DemoFromString(value);
        }
    }

    static class UnsupportedId {
    }
}
