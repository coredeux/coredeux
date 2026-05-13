package com.coredeux.demo.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk")
    private Long pk;

    @Column(name = "creationtime", nullable = false, updatable = false)
    private Instant creationTime;

    @Column(name = "modifiedtime", nullable = false)
    private Instant modifiedTime;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        creationTime = now;
        modifiedTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedTime = Instant.now();
    }
}
