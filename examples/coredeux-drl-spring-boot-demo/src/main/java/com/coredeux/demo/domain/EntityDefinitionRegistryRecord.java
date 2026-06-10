package com.coredeux.demo.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Stores the entity definition registry payload that is used to seed and
 * refresh the runtime registry.
 */
@Entity
@Table(name = "entity_definition_registry")
public class EntityDefinitionRegistryRecord extends Item {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String sourceLocation;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String yaml;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public String getYaml() {
        return yaml;
    }

    public void setYaml(String yaml) {
        this.yaml = yaml;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
