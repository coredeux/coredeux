package com.coredeux.core.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coredeux.core.definition.CoredeuxModuleDefinition;
import com.coredeux.core.definition.CoredeuxYamlConfiguration;
import com.coredeux.core.exceptions.CoredeuxValidationException;
import com.coredeux.core.registry.EntityDefinitionRegistries;
import com.coredeux.core.registry.EntityDefinitionRegistry;

class YamlEntityDefinitionLoaderTest {

    private final YamlEntityDefinitionLoader loader = new YamlEntityDefinitionLoader();

    @Test
    void shouldLoadMultipleEntityDefinitionsFromYaml() {
        CoredeuxYamlConfiguration configuration = loader.load(inputStream(validYaml()));

        assertEquals(2, configuration.getEntities().size());
        assertEquals("com.example.customer.Customer", configuration.getEntities().get(0).getFullClassName());
        assertEquals("postgresCustomerDataAccess",
                configuration.getEntities().get(0).getStorage().getDataAccessService());
        assertEquals("id", configuration.getEntities().get(0).getIdentifier());
        assertEquals(2, configuration.getEntities().get(0).getValidators().size());
        assertEquals(2, configuration.getEntities().get(0).getAttributes().size());
        assertTrue(configuration.getEntities().get(0).getAudit().isEnabled());
        assertEquals(2, configuration.getEntities().get(0).getHooks().size());
        assertEquals(4, configuration.getEntities().get(0).getModules().size());

        assertEquals("mongo", configuration.getEntities().get(1).getStorage().getStore());
        assertFalse(configuration.getEntities().get(1).getAudit().isEnabled());
        assertEquals(1, configuration.getEntities().get(1).getHooks().size());
    }

    @Test
    void shouldBuildRegistryFromYamlInputStream() {
        EntityDefinitionRegistry registry = EntityDefinitionRegistries.fromYaml(inputStream(validYaml()));

        assertEquals(2, registry.getAll().size());
        assertTrue(registry.findByFullClassName("com.example.customer.Customer").isPresent());
        assertEquals("customer",
                registry.findByFullClassName("com.example.customer.Customer").orElseThrow().getName());
    }

    @Test
    void shouldLoadFromPath() throws Exception {
        Path tempFile = Files.createTempFile("coredeux-loader", ".yml");
        Files.writeString(tempFile, validYaml(), StandardCharsets.UTF_8);

        try {
            CoredeuxYamlConfiguration configuration = loader.load(tempFile);
            assertEquals(2, configuration.getEntities().size());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldFailWhenRequiredStorageFieldIsMissing() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.customer.Customer
                      identifier: id
                      storage:
                        store: postgres
                """;

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream(yaml)));

        assertTrue(exception.getMessage().contains("data-access-service"));
    }

    @Test
    void shouldLoadEntityDefinitionWhenOptionalFieldsAreMissing() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.customer.Customer
                      identifier: id
                      storage:
                        data-access-service: postgresCustomerDataAccess
                """;

        CoredeuxYamlConfiguration configuration = loader.load(inputStream(yaml));

        assertEquals(1, configuration.getEntities().size());
        assertEquals("com.example.customer.Customer", configuration.getEntities().get(0).getFullClassName());
        assertEquals("com.example.customer.Customer", configuration.getEntities().get(0).getName());
        assertEquals("postgresCustomerDataAccess",
                configuration.getEntities().get(0).getStorage().getDataAccessService());
        assertEquals("id", configuration.getEntities().get(0).getIdentifier());
        assertEquals(null, configuration.getEntities().get(0).getStorage().getStore());
        assertTrue(configuration.getEntities().get(0).getValidators().isEmpty());
        assertTrue(configuration.getEntities().get(0).getHooks().isEmpty());
        assertTrue(configuration.getEntities().get(0).getAttributes().isEmpty());
        assertTrue(configuration.getEntities().get(0).getModules().isEmpty());
    }

    @Test
    void shouldFailWhenFullClassNameIsMissing() {
        String yaml = """
                coredeux:
                  entities:
                    - identifier: id
                      storage:
                        data-access-service: postgresCustomerDataAccess
                """;

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream(yaml)));

        assertTrue(exception.getMessage().contains("full-class-name"));
    }

    @Test
    void shouldFailWhenIdentifierIsMissing() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.customer.Customer
                      storage:
                        data-access-service: postgresCustomerDataAccess
                """;

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream(yaml)));

        assertTrue(exception.getMessage().contains("identifier"));
    }

    @Test
    void shouldLoadExplicitModuleConfigurations() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.customer.Customer
                      identifier: id
                      storage:
                        data-access-service: postgresCustomerDataAccess
                      modules:
                        - name: workflow
                          enabled: true
                          handlers:
                            - workflowHandler
                          config:
                            approval-required: true
                            process-name: customerApproval
                """;

        CoredeuxYamlConfiguration configuration = loader.load(inputStream(yaml));

        CoredeuxModuleDefinition workflow = (CoredeuxModuleDefinition) configuration.getEntities().get(0)
                .getModule("workflow");

        assertEquals("customerApproval", workflow.getConfigMap().get("process-name"));
        assertEquals(Boolean.TRUE, workflow.getConfigMap().get("approval-required"));
        assertEquals(1, workflow.getHandlers().size());
        assertEquals("workflowHandler", workflow.getHandlers().get(0));
    }

    @Test
    void shouldDeepCopyGenericModuleConfig() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.customer.Customer
                      identifier: id
                      storage:
                        data-access-service: postgresCustomerDataAccess
                      modules:
                        - name: workflow
                          config:
                            nested:
                              approvals:
                                - admin
                                - manager
                """;

        CoredeuxYamlConfiguration configuration = loader.load(inputStream(yaml));
        CoredeuxModuleDefinition workflow = configuration.getEntities().get(0).getModuleDefinition("workflow").orElseThrow();
        Map<String, Object> config = workflow.getConfigMap();

        assertTrue(config.containsKey("nested"));
        assertThrows(UnsupportedOperationException.class, () -> config.put("x", true));
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) config.get("nested");
        assertThrows(UnsupportedOperationException.class, () -> nested.put("y", true));
    }

    @Test
    void shouldFailWhenModuleNameIsMissing() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.customer.Customer
                      identifier: id
                      storage:
                        data-access-service: postgresCustomerDataAccess
                      modules:
                        - enabled: true
                          handlers:
                            - sampleHandler
                """;

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream(yaml)));

        assertTrue(exception.getMessage().contains("name"));
    }

    @Test
    void shouldDefaultModuleEnabledToTrueWhenMissing() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.customer.Customer
                      identifier: id
                      storage:
                        data-access-service: postgresCustomerDataAccess
                      modules:
                        - name: validators
                          handlers:
                            - customerRequiredFieldsValidator
                """;

        CoredeuxYamlConfiguration configuration = loader.load(inputStream(yaml));

        assertEquals(1, configuration.getEntities().get(0).getValidators().size());
        assertTrue(configuration.getEntities().get(0).getModuleDefinition("validators").orElseThrow().isEnabled());
    }

    @Test
    void shouldFailWhenYamlIsEmpty() {
        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream("")));

        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    void shouldFailWhenRootIsNotAMap() {
        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream("- just\n- a\n- list\n")));

        assertTrue(exception.getMessage().contains("root"));
    }

    @Test
    void shouldFailWhenRootKeyIsMissing() {
        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream("other:\n  entities: []\n")));

        assertTrue(exception.getMessage().contains("coredeux"));
    }

    @Test
    void shouldFailWhenEntitiesIsNotAList() {
        String yaml = """
                coredeux:
                  entities:
                    full-class-name: com.example.Customer
                """;

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream(yaml)));

        assertTrue(exception.getMessage().contains("entities"));
    }

    @Test
    void shouldFailWhenModulesIsNotAList() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.Customer
                      identifier: id
                      storage:
                        data-access-service: customerDataAccess
                      modules:
                        name: hooks
                """;

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream(yaml)));

        assertTrue(exception.getMessage().contains("modules"));
    }

    @Test
    void shouldFailWhenHandlersIsNotAList() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.Customer
                      identifier: id
                      storage:
                        data-access-service: customerDataAccess
                      modules:
                        - name: hooks
                          handlers: hookOne
                """;

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream(yaml)));

        assertTrue(exception.getMessage().contains("list of strings"));
    }

    @Test
    void shouldFailWhenAttributeConfigIsNotAList() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.Customer
                      identifier: id
                      storage:
                        data-access-service: customerDataAccess
                      modules:
                        - name: attributes
                          config:
                            name: code
                """;

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream(yaml)));

        assertTrue(exception.getMessage().contains("attributes"));
    }

    @Test
    void shouldFailWhenAttributeTypeIsMissing() {
        String yaml = """
                coredeux:
                  entities:
                    - full-class-name: com.example.Customer
                      identifier: id
                      storage:
                        data-access-service: customerDataAccess
                      modules:
                        - name: attributes
                          config:
                            - name: code
                """;

        CoredeuxValidationException exception = assertThrows(CoredeuxValidationException.class,
                () -> loader.load(inputStream(yaml)));

        assertTrue(exception.getMessage().contains("type"));
    }

    private ByteArrayInputStream inputStream(String yaml) {
        return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    }

    private String validYaml() {
        return """
                coredeux:
                  entities:
                    - full-class-name: com.example.customer.Customer
                      name: customer
                      identifier: id
                      storage:
                        store: postgres
                        data-access-service: postgresCustomerDataAccess
                      modules:
                        - name: validators
                          enabled: true
                          handlers:
                            - customerRequiredFieldsValidator
                            - customerBusinessRuleValidator
                        - name: audit
                          enabled: true
                          handlers:
                            - defaultAuditHandler
                        - name: hooks
                          enabled: true
                          handlers:
                            - customerSampleHook1
                            - customerSampleHook2
                        - name: attributes
                          enabled: true
                          config:
                            - name: firstName
                              type: STRING
                              required: true
                              searchable: true
                            - name: email
                              type: STRING
                              required: true
                              searchable: true
                    - full-class-name: com.example.audit.AuditDocument
                      name: audit-document
                      identifier: id
                      storage:
                        store: mongo
                        data-access-service: mongoAuditDataAccess
                      modules:
                        - name: audit
                          enabled: false
                          handlers:
                            - defaultAuditHandler
                        - name: hooks
                          enabled: true
                          handlers:
                            - auditBeforeSaveHook
                """;
    }
}
