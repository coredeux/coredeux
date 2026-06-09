package com.coredeux.demo.domain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void shouldPopulateCreationAndModifiedTimesOnCreate() {
        SampleItem item = new SampleItem();

        item.onCreate();

        assertNotNull(item.getCreationTime());
        assertNotNull(item.getModifiedTime());
        assertTrue(!item.getCreationTime().isAfter(Instant.now()));
    }

    @Test
    void shouldUpdateModifiedTimeOnUpdate() {
        SampleItem item = new SampleItem();
        item.onCreate();
        Instant createdAt = item.getCreatedSnapshot();

        item.onUpdate();

        assertNotNull(item.getModifiedTime());
        assertTrue(!item.getModifiedTime().isBefore(createdAt));
    }

    private static final class SampleItem extends Item {

        private Instant createdSnapshot;

        @Override
        protected void onCreate() {
            super.onCreate();
            this.createdSnapshot = getCreationTime();
        }

        Instant getCreatedSnapshot() {
            return createdSnapshot;
        }
    }
}
