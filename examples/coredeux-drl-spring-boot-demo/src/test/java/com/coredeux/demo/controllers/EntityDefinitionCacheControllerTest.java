package com.coredeux.demo.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coredeux.demo.definition.EntityDefinitionManager;
import com.coredeux.demo.domain.EntityDefinitionRegistryRecord;

@ExtendWith(MockitoExtension.class)
class EntityDefinitionCacheControllerTest {

    @Mock
    private EntityDefinitionManager manager;

    @Test
    void exposesStatusRefreshBootstrapUpdateAndPurgeOperations() {
        EntityDefinitionCacheController controller = new EntityDefinitionCacheController(manager);
        EntityDefinitionRegistryRecord record = new EntityDefinitionRegistryRecord();
        record.setCode("coredeux-demo");
        record.setSourceLocation("postman");
        record.setYaml("coredeux:\n  entities: []\n");
        record.setUpdatedAt(Instant.parse("2026-06-11T00:00:00Z"));

        when(manager.isCached()).thenReturn(true);
        when(manager.findRecord()).thenReturn(Optional.of(record));
        when(manager.updateEntityDefinitionFromFile()).thenReturn(record);
        when(manager.updateDefinition("yaml", "postman")).thenReturn(record);
        when(manager.getCachedYML()).thenReturn(record.getYaml());

        Map<String, Object> status = controller.status();
        assertEquals(true, status.get("cached"));
        assertEquals(1, status.get("records"));
        assertEquals("coredeux-demo", status.get("registryCode"));
        assertEquals(record.getYaml(), controller.getYML());

        Map<String, Object> refreshed = controller.refresh();
        assertEquals(true, refreshed.get("refreshed"));
        assertEquals(true, refreshed.get("cached"));
        verify(manager).refreshCache();

        Map<String, Object> bootstrapped = controller.bootstrap();
        assertEquals(true, bootstrapped.get("bootstrapped"));
        assertEquals(true, bootstrapped.get("cached"));
        verify(manager).updateEntityDefinitionFromFile();

        Map<String, Object> updated = controller.update("yaml", "postman");
        assertEquals(true, updated.get("updated"));
        assertEquals("coredeux-demo", updated.get("code"));
        assertEquals("postman", updated.get("sourceLocation"));
        verify(manager).updateDefinition("yaml", "postman");

        assertEquals(204, controller.purge().getStatusCode().value());
        verify(manager).purgeCache();
    }
}
