package com.coredeux.demo.drl;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coredeux.demo.domain.DrlRuleRecord;

public interface DrlRuleRecordRepository extends JpaRepository<DrlRuleRecord, Long> {

    Optional<DrlRuleRecord> findByCode(String code);
}
