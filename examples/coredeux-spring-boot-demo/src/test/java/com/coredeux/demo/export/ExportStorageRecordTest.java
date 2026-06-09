package com.coredeux.demo.export;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class ExportStorageRecordTest {

    @Test
    void shouldPopulateCreatedAtOnPersistHook() throws Exception {
        ExportStorageRecord record = ExportStorageRecord.builder().build();
        Method prePersist = ExportStorageRecord.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(record);
        assertNotNull(record.getCreatedAt());
    }
}
