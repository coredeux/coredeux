package com.coredeux.examples.nativejava.drl;

public class DrlRuleNotFoundException extends RuntimeException {

    public DrlRuleNotFoundException(String code) {
        super("DRL rule not found for code: " + code);
    }
}
