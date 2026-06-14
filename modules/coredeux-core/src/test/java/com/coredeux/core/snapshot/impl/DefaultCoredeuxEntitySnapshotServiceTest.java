package com.coredeux.core.snapshot.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DefaultCoredeuxEntitySnapshotServiceTest {

    private final DefaultCoredeuxEntitySnapshotService service = new DefaultCoredeuxEntitySnapshotService();

    @Test
    void shouldReturnNullForNullValue() {
        assertNull(service.snapshot(null));
    }

    @Test
    void shouldReuseImmutableValues() {
        assertSame("value", service.snapshot("value"));
        assertSame(BigDecimal.TEN, service.snapshot(BigDecimal.TEN));
        Instant instant = Instant.now();
        assertSame(instant, service.snapshot(instant));
        assertSame(SampleEnum.ACTIVE, service.snapshot(SampleEnum.ACTIVE));
        assertSame(String.class, service.snapshot(String.class));
    }

    @Test
    void shouldCopyDateValues() {
        Date source = new Date(1000);

        Date snapshot = service.snapshot(source);
        source.setTime(2000);

        assertNotSame(source, snapshot);
        assertEquals(1000, snapshot.getTime());
    }

    @Test
    void shouldCopyTopLevelBeanIncludingPrivateAndInheritedFields() {
        SnapshotEntity source = new SnapshotEntity();
        source.id = "1";
        source.name = "before";
        source.setInherited("parent");

        SnapshotEntity snapshot = service.snapshot(source);
        source.id = "2";
        source.name = "after";
        source.setInherited("changed");

        assertNotSame(source, snapshot);
        assertEquals("1", snapshot.id);
        assertEquals("before", snapshot.name);
        assertEquals("parent", snapshot.getInherited());
    }

    @Test
    void shouldIgnoreStaticAndTransientFields() {
        SnapshotEntity.staticValue = "static-before";
        SnapshotEntity source = new SnapshotEntity();
        source.transientValue = "transient-before";

        SnapshotEntity snapshot = service.snapshot(source);
        SnapshotEntity.staticValue = "static-after";
        source.transientValue = "transient-after";

        assertEquals("static-after", SnapshotEntity.staticValue);
        assertNull(snapshot.transientValue);
    }

    @Test
    void shouldDefensivelyCopyImmediateCollectionFields() {
        SnapshotEntity source = new SnapshotEntity();
        NestedEntity nested = new NestedEntity("child");
        source.list = new ArrayList<>(List.of("one", nested));
        source.set = new LinkedHashSet<>(Set.of("alpha"));
        source.queue = new ArrayDeque<>(List.of("first"));

        SnapshotEntity snapshot = service.snapshot(source);
        source.list.add("two");
        source.set.add("beta");
        source.queue.add("second");
        nested.value = "mutated";

        assertNotSame(source.list, snapshot.list);
        assertNotSame(source.set, snapshot.set);
        assertNotSame(source.queue, snapshot.queue);
        assertEquals(List.of("one", nested), snapshot.list);
        assertEquals(Set.of("alpha"), snapshot.set);
        assertEquals(List.of("first"), List.copyOf(snapshot.queue));
        assertSame(nested, snapshot.list.get(1));
        assertEquals("mutated", ((NestedEntity) snapshot.list.get(1)).value);
    }

    @Test
    void shouldDefensivelyCopyImmediateMapFields() {
        SnapshotEntity source = new SnapshotEntity();
        NestedEntity key = new NestedEntity("key");
        NestedEntity value = new NestedEntity("value");
        source.map = new LinkedHashMap<>();
        source.map.put("simple", "one");
        source.map.put(key, value);

        SnapshotEntity snapshot = service.snapshot(source);
        source.map.put("new", "two");
        key.value = "key-mutated";
        value.value = "value-mutated";

        assertNotSame(source.map, snapshot.map);
        assertEquals(2, snapshot.map.size());
        assertSame(value, snapshot.map.get(key));
        assertEquals("value-mutated", ((NestedEntity) snapshot.map.get(key)).value);
    }

    @Test
    void shouldDefensivelyCopyImmediateArrayFields() {
        SnapshotEntity source = new SnapshotEntity();
        NestedEntity nested = new NestedEntity("array-child");
        source.names = new String[] { "one", "two" };
        source.children = new NestedEntity[] { nested };

        SnapshotEntity snapshot = service.snapshot(source);
        source.names[0] = "changed";
        source.children[0] = new NestedEntity("replacement");
        nested.value = "nested-mutated";

        assertNotSame(source.names, snapshot.names);
        assertNotSame(source.children, snapshot.children);
        assertArrayEquals(new String[] { "one", "two" }, snapshot.names);
        assertSame(nested, snapshot.children[0]);
        assertEquals("nested-mutated", snapshot.children[0].value);
    }

    @Test
    void shouldCopyRootCollectionMapAndArrayValues() {
        List<String> list = new ArrayList<>(List.of("one"));
        Map<String, String> map = new LinkedHashMap<>(Map.of("key", "value"));
        String[] array = new String[] { "a" };

        List<String> listSnapshot = service.snapshot(list);
        Map<String, String> mapSnapshot = service.snapshot(map);
        String[] arraySnapshot = service.snapshot(array);
        list.add("two");
        map.put("new", "changed");
        array[0] = "b";

        assertEquals(List.of("one"), listSnapshot);
        assertEquals(Map.of("key", "value"), mapSnapshot);
        assertArrayEquals(new String[] { "a" }, arraySnapshot);
    }

    @Test
    void shouldKeepNestedBeanReferencesAsLevelOneSnapshotPolicy() {
        SnapshotEntity source = new SnapshotEntity();
        NestedEntity nested = new NestedEntity("before");
        source.nested = nested;

        SnapshotEntity snapshot = service.snapshot(source);
        nested.value = "after";

        assertSame(nested, snapshot.nested);
        assertEquals("after", snapshot.nested.value);
    }

    @Test
    void shouldReturnOriginalWhenTypeCannotBeInstantiated() {
        NoDefaultConstructor source = new NoDefaultConstructor("value");

        NoDefaultConstructor snapshot = service.snapshot(source);

        assertSame(source, snapshot);
    }

    private enum SampleEnum {
        ACTIVE
    }

    private static class ParentEntity {
        private String inherited;

        protected String getInherited() {
            return inherited;
        }

        protected void setInherited(String inherited) {
            this.inherited = inherited;
        }
    }

    private static final class SnapshotEntity extends ParentEntity {

        private static String staticValue;

        private String id;
        private String name;
        private transient String transientValue;
        private List<Object> list;
        private Set<Object> set;
        private Queue<Object> queue;
        private Map<Object, Object> map;
        private String[] names;
        private NestedEntity[] children;
        private NestedEntity nested;
    }

    private static final class NestedEntity {

        private String value;

        private NestedEntity(String value) {
            this.value = value;
        }
    }

    private static final class NoDefaultConstructor {

        @SuppressWarnings("unused")
        private final String value;

        private NoDefaultConstructor(String value) {
            this.value = value;
        }
    }
}
