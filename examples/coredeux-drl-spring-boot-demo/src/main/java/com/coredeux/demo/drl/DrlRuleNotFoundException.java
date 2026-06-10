package com.coredeux.demo.drl;

public class DrlRuleNotFoundException extends RuntimeException {

    public DrlRuleNotFoundException(String ruleId) {
        super("No DRL rule found for code: " + ruleId);
    }
}
