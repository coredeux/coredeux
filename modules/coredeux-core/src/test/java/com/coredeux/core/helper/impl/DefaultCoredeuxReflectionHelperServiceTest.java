package com.coredeux.core.helper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.coredeux.core.exceptions.CoredeuxStrategyException;

class DefaultCoredeuxReflectionHelperServiceTest {

    private final DefaultCoredeuxReflectionHelperService helperService = new DefaultCoredeuxReflectionHelperService();

    @Test
    void shouldReadPropertyUsingGetterWhenAvailable() {
        assertEquals("123", helperService.getFieldValue("id", new ChildSample("123", "value")));
    }

    @Test
    void shouldReadInheritedFieldWhenGetterIsMissing() {
        assertEquals("parent-value", helperService.getFieldValue("parentOnly", new ChildSample("123", "value")));
    }

    @Test
    void shouldReadFieldWhenPropertyIsWriteOnly() {
        WriteOnlySample sample = new WriteOnlySample();
        sample.writeOnly = "write-only-value";

        assertEquals("write-only-value", helperService.getFieldValue("writeOnly", sample, WriteOnlySample.class));
    }

    @Test
    void shouldReturnNullForMissingInputWhenReadingFieldValue() {
        assertNull(helperService.getFieldValue("id", null));
        assertNull(helperService.getFieldValue(null, new ChildSample("123", "value"), ChildSample.class));
        assertNull(helperService.getFieldValue(" ", new ChildSample("123", "value"), ChildSample.class));
        assertNull(helperService.getFieldValue("id", null, ChildSample.class));
    }

    @Test
    void shouldSetFieldValue() {
        MutableSample sample = new MutableSample();
        Field field = helperService.getDeclaredField("value", MutableSample.class);

        helperService.setFieldValue(field, sample, "updated");

        assertEquals("updated", sample.getValue());
    }

    @Test
    void shouldFailWhenFieldIsNullWhileSettingFieldValue() {
        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> helperService.setFieldValue(null, new MutableSample(), "updated"));

        assertTrue(exception.getMessage().contains("Field must not be null"));
    }

    @Test
    void shouldReturnAllFieldsAcrossHierarchy() {
        List<Field> fields = helperService.getAllFields(ChildSample.class);

        assertEquals(3, fields.size());
        assertTrue(fields.stream().anyMatch(field -> "id".equals(field.getName())));
        assertTrue(fields.stream().anyMatch(field -> "value".equals(field.getName())));
        assertTrue(fields.stream().anyMatch(field -> "parentOnly".equals(field.getName())));
    }

    @Test
    void shouldReturnAllFieldsForClassWithoutHierarchy() {
        List<Field> fields = helperService.getAllFields(MutableSample.class);

        assertEquals(1, fields.size());
        assertEquals("value", fields.get(0).getName());
    }

    @Test
    void shouldReturnEmptyFieldsForNullType() {
        assertTrue(helperService.getAllFields(null).isEmpty());
    }

    @Test
    void shouldResolveClassByName() {
        Class<?> type = helperService.getClass(ChildSample.class.getName());

        assertEquals(ChildSample.class, type);
    }

    @Test
    void shouldFailWhenClassCannotBeResolved() {
        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> helperService.getClass("com.example.MissingType"));

        assertTrue(exception.getMessage().contains("Unable to resolve class"));
    }

    @Test
    void shouldFailWhenFieldCannotBeResolved() {
        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> helperService.getFieldValue("missing", new ChildSample("123", "value")));

        assertTrue(exception.getMessage().contains("missing"));
    }

    @Test
    void shouldFailWhenGetterInvocationFails() {
        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> helperService.getFieldValue("value", new ThrowingGetterSample(), ThrowingGetterSample.class));

        assertTrue(exception.getMessage().contains("Unable to read property 'value'"));
    }

    @Test
    void shouldFailWhenStaticFinalFieldCannotBeSet() throws Exception {
        Field field = StaticFinalSample.class.getDeclaredField("CONSTANT");

        CoredeuxStrategyException exception = assertThrows(CoredeuxStrategyException.class,
                () -> helperService.setFieldValue(field, null, "updated"));

        assertTrue(exception.getMessage().contains("Unable to set field 'CONSTANT'"));
    }

    @Test
    void shouldFindDeclaredFieldInParentHierarchy() {
        Field field = helperService.getDeclaredField("parentOnly", ChildSample.class);

        assertNotNull(field);
        assertEquals("parentOnly", field.getName());
    }

    @Test
    void shouldReturnNullWhenDeclaredFieldIsMissing() {
        assertNull(helperService.getDeclaredField("missing", ChildSample.class));
        assertNull(helperService.getDeclaredField("missing", null));
    }

    private static class ParentSample {

        private final String parentOnly = "parent-value";
    }

    private static final class ChildSample extends ParentSample {

        private final String id;
        private final String value;

        private ChildSample(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }
    }

    private static final class MutableSample {

        private String value;

        public String getValue() {
            return value;
        }
    }

    private static final class WriteOnlySample {

        private String writeOnly;

        @SuppressWarnings("unused")
        public void setWriteOnly(String writeOnly) {
            this.writeOnly = writeOnly;
        }
    }

    private static final class ThrowingGetterSample {

        public String getValue() {
            throw new IllegalStateException("boom");
        }
    }

    private static final class StaticFinalSample {

        private static final String CONSTANT = "initial";
    }
}
