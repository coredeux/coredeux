package com.coredeux.demo.definition;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coredeux.demo.domain.EntityDefinitionRegistryRecord;

public interface EntityDefinitionRegistryRecordRepository extends JpaRepository<EntityDefinitionRegistryRecord, Long> {

    Optional<EntityDefinitionRegistryRecord> findByCode(String code);
}
