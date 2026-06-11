package com.coredeux.demo.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.doReturn;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.demo.definition.DemoEntityResolver;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CoredeuxCrudControllerTest {

    @Mock
    private CoredeuxService coredeuxService;

    @Mock
    private DemoEntityResolver demoEntityResolver;

    @Test
    void createsReadsListsUpdatesAndDeletesEntities() {
        ObjectMapper objectMapper = new ObjectMapper();
        CoredeuxCrudController controller = new CoredeuxCrudController(coredeuxService, demoEntityResolver, objectMapper);
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName(SampleEntity.class.getName())
                .name("sample")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().dataAccessService("sampleDataAccess").build())
                .build();
        SampleEntity created = new SampleEntity();
        created.setId("1");
        created.setName("Alice Example");
        created.setEmail("alice@example.com");
        created.setActive(true);
        created.setStatus("ACTIVE");

        when(demoEntityResolver.resolveDefinition("sample")).thenReturn(definition);
        doReturn(SampleEntity.class).when(demoEntityResolver).resolveType("sample");
        doReturn(String.class).when(demoEntityResolver).resolveIdentifierType(SampleEntity.class, definition);
        when(coredeuxService.save(any())).thenReturn("1");
        when(coredeuxService.load("1", SampleEntity.class)).thenReturn(created);
        when(coredeuxService.loadAll(eq(List.of()), eq(SampleEntity.class), eq(20), eq(1)))
                .thenReturn(SearchResult.<SampleEntity>builder().results(List.of(created)).build());

        Map<String, Object> payload = Map.of(
                "name", "Alice Example",
                "email", "alice@example.com",
                "active", true,
                "status", "ACTIVE");

        var createdResponse = controller.create("sample", payload);
        assertEquals(201, createdResponse.getStatusCode().value());
        assertEquals(created, createdResponse.getBody());
        assertEquals(created, controller.read("sample", "1").getBody());
        assertEquals(1, ((SearchResult<?>) controller.list("sample", 20, 1).getBody()).getResults().size());
        var updatedResponse = controller.update("sample", "1", payload);
        assertEquals(200, updatedResponse.getStatusCode().value());
        assertEquals(created, updatedResponse.getBody());
        assertEquals(204, controller.delete("sample", "1").getStatusCode().value());

        verify(coredeuxService).save(any());
        verify(coredeuxService).update(any());
        verify(coredeuxService).remove("1", SampleEntity.class);
    }

    private static final class SampleEntity {

        private String id;
        private String name;
        private String email;
        private boolean active;
        private String status;

        public SampleEntity() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
