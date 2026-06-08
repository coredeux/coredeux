package com.coredeux.demo.drl;

public class DrlRuleNotFoundException extends RuntimeException {

    public DrlRuleNotFoundException(String code) {
        super("DRL rule not found for code: " + code);
    }
}
