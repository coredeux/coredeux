package com.coredeux.demo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.service.CoredeuxService;
import com.coredeux.demo.domain.Customer;
import com.coredeux.demo.domain.CustomerStatus;

@WebMvcTest(CoredeuxCrudController.class)
@Import({CoredeuxDemoExceptionHandler.class, CoredeuxCrudControllerTest.TestConfig.class})
class CoredeuxCrudControllerTest {

    private static final String CUSTOMER_ENTITY = Customer.class.getName();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoredeuxService coredeuxService;

    @MockBean
    private DemoEntityResolver demoEntityResolver;

    @Test
    void shouldCreateEntityThroughCoredeuxService() throws Exception {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().name("customer").identifier("pk").build();
        Customer created = sampleCustomer(1L, "Alice");

        when(demoEntityResolver.resolveDefinition(CUSTOMER_ENTITY)).thenReturn(definition);
        doReturn(Customer.class).when(demoEntityResolver).resolveType(CUSTOMER_ENTITY);
        when(coredeuxService.save(any(Customer.class))).thenReturn("1");
        when(coredeuxService.load("1", Customer.class)).thenReturn(created);

        mockMvc.perform(post("/api/entities/{entityName}", CUSTOMER_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"status\":\"ACTIVE\",\"active\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pk").value(1))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void shouldReadEntity() throws Exception {
        doReturn(Customer.class).when(demoEntityResolver).resolveType(CUSTOMER_ENTITY);
        when(coredeuxService.load("1", Customer.class)).thenReturn(sampleCustomer(1L, "Alice"));

        mockMvc.perform(get("/api/entities/{entityName}/{id}", CUSTOMER_ENTITY, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pk").value(1))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void shouldListEntities() throws Exception {
        doReturn(Customer.class).when(demoEntityResolver).resolveType(CUSTOMER_ENTITY);
        when(coredeuxService.loadAll(List.of(), Customer.class, 5, 2))
                .thenReturn(SearchResult.<Customer>builder().results(List.of(sampleCustomer(1L, "Alice"))).build());

        mockMvc.perform(get("/api/entities/{entityName}", CUSTOMER_ENTITY)
                        .param("pageSize", "5")
                        .param("currentPage", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].pk").value(1));
    }

    @Test
    void shouldUpdateEntityThroughCoredeuxService() throws Exception {
        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder().name("customer").identifier("pk").build();
        Customer updated = sampleCustomer(1L, "Updated");

        when(demoEntityResolver.resolveDefinition(CUSTOMER_ENTITY)).thenReturn(definition);
        doReturn(Customer.class).when(demoEntityResolver).resolveType(CUSTOMER_ENTITY);
        doReturn(Long.class).when(demoEntityResolver).resolveIdentifierType(Customer.class, definition);
        doAnswer(invocation -> {
            Customer entity = invocation.getArgument(0);
            Long identifier = invocation.getArgument(2);
            entity.setPk(identifier);
            return null;
        }).when(demoEntityResolver).applyIdentifier(any(Customer.class), eq(definition), anyLong());
        when(coredeuxService.load("1", Customer.class)).thenReturn(updated);
        doNothing().when(coredeuxService).update(any(Customer.class));

        mockMvc.perform(put("/api/entities/{entityName}/{id}", CUSTOMER_ENTITY, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"email\":\"alice@example.com\",\"status\":\"ACTIVE\",\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pk").value(1))
                .andExpect(jsonPath("$.name").value("Updated"));

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(coredeuxService).update(customerCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1L, customerCaptor.getValue().getPk());
    }

    @Test
    void shouldDeleteEntity() throws Exception {
        doReturn(Customer.class).when(demoEntityResolver).resolveType(CUSTOMER_ENTITY);
        doNothing().when(coredeuxService).remove("1", Customer.class);

        mockMvc.perform(delete("/api/entities/{entityName}/{id}", CUSTOMER_ENTITY, "1"))
                .andExpect(status().isNoContent());

        verify(coredeuxService).remove("1", Customer.class);
    }

    @Test
    void shouldReturnBadRequestForValidationFailure() throws Exception {
        when(demoEntityResolver.resolveDefinition("missing"))
                .thenThrow(new CoredeuxValidationException("Unknown entity: missing"));

        mockMvc.perform(post("/api/entities/{entityName}", "missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown entity: missing"));
    }

    private Customer sampleCustomer(Long pk, String name) {
        return Customer.builder()
                .pk(pk)
                .name(name)
                .email("alice@example.com")
                .status(CustomerStatus.ACTIVE)
                .active(true)
                .build();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        }
    }
}
