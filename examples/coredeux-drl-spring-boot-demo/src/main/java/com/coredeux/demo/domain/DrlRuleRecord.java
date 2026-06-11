package com.coredeux.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "drl_rules")
public class DrlRuleRecord extends Item {

    @Column(nullable = false, unique = true)
    private String code;

    @Column
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String drl;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDrl() {
        return drl;
    }

    public void setDrl(String drl) {
        this.drl = drl;
    }
}
