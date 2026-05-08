package com.coredeux.demo.export;

import java.time.Instant;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "export_storage_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportStorageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uid;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String storageType;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] content;

    @Lob
    private String metadataJson;

    private String url;

    private String canonicalUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
