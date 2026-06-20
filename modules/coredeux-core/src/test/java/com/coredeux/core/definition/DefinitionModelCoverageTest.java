package com.coredeux.core.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DefinitionModelCoverageTest {

    @Test
    void shouldExerciseEntityDefinitionModuleHelpers() {
        CoredeuxAttributeDefinition attribute = CoredeuxAttributeDefinition.builder()
                .name("email")
                .type("STRING")
                .required(true)
                .searchable(true)
                .validators(List.of("requiredValidator", "formatValidator"))
                .build();

        CoredeuxModuleDefinition auditModule = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("auditHandler"))
                .config(Map.of("mode", "full"))
                .build();
        CoredeuxModuleDefinition validatorModule = CoredeuxModuleDefinition.builder()
                .name("validators")
                .enabled(true)
                .handlers(List.of("validatorOne", "validatorTwo"))
                .build();
        CoredeuxModuleDefinition hookModule = CoredeuxModuleDefinition.builder()
                .name("hooks")
                .enabled(false)
                .handlers(List.of("beforeSaveHook"))
                .build();
        CoredeuxModuleDefinition attributeModule = CoredeuxModuleDefinition.builder()
                .name("attributes")
                .enabled(true)
                .config(List.of(attribute))
                .build();

        CoredeuxEntityDefinition definition = CoredeuxEntityDefinition.builder()
                .fullClassName("com.example.Customer")
                .name("customer")
                .identifier("id")
                .storage(CoredeuxStorageDefinition.builder().store("postgres").dataAccessService("customerDataAccess")
                        .build())
                .modules(List.of(auditModule, validatorModule, hookModule, attributeModule))
                .build();

        assertSame(auditModule, definition.getAudit());
        assertEquals(List.of("validatorOne", "validatorTwo"), definition.getValidators());
        assertTrue(definition.getHooks().isEmpty());
        assertEquals(1, definition.getAttributes().size());
        assertEquals("email", definition.getAttributes().get(0).getName());
        assertSame(validatorModule, definition.getModule("validators"));
        assertTrue(definition.getModuleDefinition("hooks").isPresent());
        assertFalse(definition.getModuleDefinition("missing").isPresent());
        assertNull(definition.getModule("missing"));
        assertEquals("com.example.Customer", definition.getFullClassName());
        assertEquals("customer", definition.getName());
        assertEquals("id", definition.getIdentifier());
        assertNotNull(definition.toString());
    }

    @Test
    void shouldReturnDefaultsForMissingOrInvalidModuleConfigurations() {
        CoredeuxModuleDefinition invalidAttributeModule = CoredeuxModuleDefinition.builder()
                .name("attributes")
                .enabled(true)
                .config(Map.of("unexpected", true))
                .build();
        CoredeuxEntityDefinition noModules = CoredeuxEntityDefinition.builder()
                .fullClassName("com.example.Empty")
                .modules(null)
                .build();
        CoredeuxEntityDefinition invalidModules = CoredeuxEntityDefinition.builder()
                .fullClassName("com.example.Invalid")
                .modules(List.of(
                        CoredeuxModuleDefinition.builder().name("validators").enabled(false)
                                .handlers(List.of("validator")).build(),
                        invalidAttributeModule))
                .build();

        assertNull(noModules.getAudit());
        assertTrue(noModules.getValidators().isEmpty());
        assertTrue(noModules.getHooks().isEmpty());
        assertTrue(noModules.getAttributes().isEmpty());
        assertTrue(invalidModules.getValidators().isEmpty());
        assertTrue(invalidModules.getAttributes().isEmpty());
    }

    @Test
    void shouldExerciseModuleDefinitionHelpersAndEquality() {
        CoredeuxModuleDefinition withMapConfig = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("auditHandler"))
                .config(Map.of("enabled", true, "mode", "full"))
                .build();
        CoredeuxModuleDefinition sameDefinition = CoredeuxModuleDefinition.builder()
                .name("audit")
                .enabled(true)
                .handlers(List.of("auditHandler"))
                .config(Map.of("enabled", true, "mode", "full"))
                .build();
        CoredeuxModuleDefinition nonMapConfig = CoredeuxModuleDefinition.builder()
                .name("attributes")
                .enabled(true)
                .config(List.of("value"))
                .build();

        assertEquals(Boolean.TRUE, withMapConfig.getConfigMap().get("enabled"));
        assertEquals("full", withMapConfig.getConfigMap().get("mode"));
        assertTrue(nonMapConfig.getConfigMap().isEmpty());
        assertEquals(withMapConfig, sameDefinition);
        assertEquals(withMapConfig.hashCode(), sameDefinition.hashCode());
        assertNotEquals(withMapConfig, nonMapConfig);
        assertNotNull(withMapConfig.toString());
        assertEquals("audit", withMapConfig.getName());
        assertTrue(withMapConfig.isEnabled());
        assertEquals(1, withMapConfig.getHandlers().size());
    }

    @Test
    void shouldExerciseSimpleDefinitionModels() {
        CoredeuxAttributeDefinition attribute = CoredeuxAttributeDefinition.builder()
                .name("status")
                .type("ENUM")
                .required(false)
                .searchable(false)
                .validators(null)
                .build();
        CoredeuxAttributeDefinition sameAttribute = CoredeuxAttributeDefinition.builder()
                .name("status")
                .type("ENUM")
                .required(false)
                .searchable(false)
                .validators(null)
                .build();
        CoredeuxAuditDefinition auditDefinition = CoredeuxAuditDefinition.builder()
                .enabled(true)
                .handler("defaultAuditHandler")
                .build();
        CoredeuxAuditDefinition sameAudit = CoredeuxAuditDefinition.builder()
                .enabled(true)
                .handler("defaultAuditHandler")
                .build();
        CoredeuxStorageDefinition storageDefinition = CoredeuxStorageDefinition.builder()
                .store("mongo")
                .identifier("documentId")
                .dataAccessService("mongoDataAccess")
                .build();
        CoredeuxStorageDefinition sameStorage = CoredeuxStorageDefinition.builder()
                .store("mongo")
                .identifier("documentId")
                .dataAccessService("mongoDataAccess")
                .build();
        CoredeuxYamlConfiguration yamlConfiguration = CoredeuxYamlConfiguration.builder()
                .entities(List.of(CoredeuxEntityDefinition.builder().fullClassName("com.example.Customer").build()))
                .build();
        CoredeuxYamlConfiguration sameYamlConfiguration = CoredeuxYamlConfiguration.builder()
                .entities(List.of(CoredeuxEntityDefinition.builder().fullClassName("com.example.Customer").build()))
                .build();

        assertEquals("status", attribute.getName());
        assertEquals("ENUM", attribute.getType());
        assertFalse(attribute.isRequired());
        assertFalse(attribute.isSearchable());
        assertNull(attribute.getValidators());
        assertEquals(attribute, sameAttribute);
        assertEquals(attribute.hashCode(), sameAttribute.hashCode());
        assertNotNull(attribute.toString());

        assertTrue(auditDefinition.isEnabled());
        assertEquals("defaultAuditHandler", auditDefinition.getHandler());
        assertEquals(auditDefinition, sameAudit);
        assertEquals(auditDefinition.hashCode(), sameAudit.hashCode());
        assertNotNull(auditDefinition.toString());

        assertEquals("mongo", storageDefinition.getStore());
        assertEquals("documentId", storageDefinition.getIdentifier());
        assertEquals("mongoDataAccess", storageDefinition.getDataAccessService());
        assertEquals(storageDefinition, sameStorage);
        assertEquals(storageDefinition.hashCode(), sameStorage.hashCode());
        assertNotNull(storageDefinition.toString());

        assertEquals(1, yamlConfiguration.getEntities().size());
        assertEquals(yamlConfiguration, sameYamlConfiguration);
        assertEquals(yamlConfiguration.hashCode(), sameYamlConfiguration.hashCode());
        assertNotNull(yamlConfiguration.toString());
    }
}
