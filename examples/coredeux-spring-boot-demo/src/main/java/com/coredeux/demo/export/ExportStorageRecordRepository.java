package com.coredeux.demo.export;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportStorageRecordRepository extends JpaRepository<ExportStorageRecord, Long> {

    Optional<ExportStorageRecord> findByUid(String uid);
}
