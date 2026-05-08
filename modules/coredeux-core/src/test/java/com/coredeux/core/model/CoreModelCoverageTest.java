package com.coredeux.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.context.EntityLifecycleContext;
import com.coredeux.core.context.OperationContext;
import com.coredeux.core.context.RequestContext;
import com.coredeux.core.definition.CoredeuxAttributeDefinition;
import com.coredeux.core.definition.CoredeuxEntityDefinition;
import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.definition.CoredeuxStorageDefinition;
import com.coredeux.core.search.PaginationData;
import com.coredeux.core.search.SearchParams;
import com.coredeux.core.search.SearchResult;
import com.coredeux.core.validation.ValidationError;

class CoreModelCoverageTest {

    @Test
    void shouldExerciseEntityDefinitionConvenienceMethods() {
        CoredeuxAttributeDefinition attribute = CoredeuxAttributeDefinition.builder()
                .name("name")
                .type("STRING")
                .required(true)
                .searchable(true)
                .validators(List.of("requiredValidator"))
                .build();

        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName("com.example.Customer")
                .name("customer")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().store("postgres").dataAccessService("customerDataAccess")
                        .build())
                .modules(List.of(
                        CoredeuxModuleDefinition.builder().name("audit").enabled(true)
                                .handlers(List.of("auditHandler")).config(Map.of("enabled", true)).build(),
                        CoredeuxModuleDefinition.builder().name("validators").enabled(true)
                                .handlers(List.of("validatorOne", "validatorTwo")).build(),
                        CoredeuxModuleDefinition.builder().name("hooks").enabled(false)
                                .handlers(List.of("hookOne")).build(),
                        CoredeuxModuleDefinition.builder().name("attributes").enabled(true).config(List.of(attribute))
                                .build()))
                .build();

        assertNotNull(definition.getAudit());
        assertEquals(2, definition.getValidators().size());
        assertTrue(definition.getHooks().isEmpty());
        assertEquals(1, definition.getAttributes().size());
        assertNotNull(definition.getModule("audit"));
        assertTrue(definition.getModuleDefinition("validators").isPresent());
        assertFalse(CoredeuxEntityDefinition.builder().fullClassName("type").modules(null).build()
                .getModuleDefinition("missing").isPresent());
    }

    @Test
    void shouldExerciseContextAndSearchModels() {
        RequestContext requestContext = RequestContext.builder()
                .requestId("req-1")
                .correlationId("corr-1")
                .userId("user-1")
                .tenantId("tenant-1")
                .build();
        EntityLifecycleContext<String> lifecycleContext = EntityLifecycleContext.<String>builder()
                .operation("UPSERT")
                .identifier("1")
                .oldValue("old")
                .newValue("new")
                .build();
        OperationContext operationContext = OperationContext.builder()
                .invokedAt(Instant.now())
                .requestContext(requestContext)
                .build();
        OperationContext updatedContext = operationContext.withLifecycleContext(lifecycleContext);

        assertNotNull(updatedContext.getInvokedAt());
        assertEquals(lifecycleContext, updatedContext.getLifecycleContext());
        assertEquals("UPSERT", updatedContext.getLifecycleContext().getOperation());
        assertNotNull(OperationContext.empty());

        SearchParams params = SearchParams.builder().field("name").comparator("eq").value("john").build();
        PaginationData pagination = PaginationData.builder().currentPage(1L).pageSize(10L).resultSize(1L)
                .totalPages(1L).totalResults(1L).build();
        SearchResult<String> result = SearchResult.<String>builder().results(List.of("john")).pagination(pagination)
                .build();
        ValidationError error = ValidationError.builder().field("name").message("required").build();

        assertEquals("name", params.getField());
        assertEquals(1L, pagination.getCurrentPage());
        assertEquals(1, result.getResults().size());
        assertEquals("required", error.getMessage());
    }

    @Test
    void shouldExerciseModuleAndStorageHelpers() {
        CoredeuxModuleDefinition module = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("auditHandler"))
                .config(Map.of("enabled", true))
                .build();
        CoredeuxStorageDefinition storage = CoredeuxStorageDefinition.builder().store("mongo")
                .dataAccessService("mongoDataAccess").build();

        assertEquals(Boolean.TRUE, module.getConfigMap().get("enabled"));
        assertTrue(CoredeuxModuleDefinition.builder().name("empty").build().getConfigMap().isEmpty());
        assertEquals("mongo", storage.getStore());
        assertEquals("mongoDataAccess", storage.getDataAccessService());
        assertNull(CoredeuxAttributeDefinition.builder().name("x").type("STRING").build().getValidators());
    }
}
