package com.coredeux.examples.nativejava.drl;

import com.coredeux.drl.model.RuleContext;

public class DrlSourceExecutionRequest {

    private String source;
    private RuleContext context;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public RuleContext getContext() {
        return context;
    }

    public void setContext(RuleContext context) {
        this.context = context;
    }
}
