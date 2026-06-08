package com.coredeux.demo.drl;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DrlRuleRecordRepository extends JpaRepository<DrlRuleRecord, Long> {

    Optional<DrlRuleRecord> findByCode(String code);

    boolean existsByCode(String code);
}
