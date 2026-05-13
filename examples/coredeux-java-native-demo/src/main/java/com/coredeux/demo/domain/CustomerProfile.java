package com.coredeux.demo.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CustomerProfile extends Item {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    @JsonBackReference("customer-profile")
    private Customer customer;

    @Column(length = 1000)
    private String biography;

    private Boolean marketingOptIn;

    /**
     * Legacy-style date field used to demonstrate custom handler parsing with a
     * caller-supplied date format.
     */
    private Date legacySignupDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_profile_tags", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> favoriteTags = new LinkedHashSet<>();
}
