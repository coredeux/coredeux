package com.coredeux.drl.converter;

public class ConvertedDrl {

    private final String ruleId;
    private final String drl;

    public ConvertedDrl(String ruleId, String drl) {
        this.ruleId = ruleId;
        this.drl = drl;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getDrl() {
        return drl;
    }
}
