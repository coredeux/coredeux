package com.coredeux.demo.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void initializesCreationAndModifiedTimestamps() {
        TestItem item = new TestItem();

        item.onCreate();

        assertNotNull(item.getCreationTime());
        assertNotNull(item.getModifiedTime());

        Instant beforeUpdate = item.getModifiedTime();
        item.onUpdate();

        assertTrue(item.getModifiedTime().compareTo(beforeUpdate) >= 0);
    }

    private static final class TestItem extends Item {
    }
}
