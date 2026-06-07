package com.coredeux.drl.model;

import java.util.HashMap;
import java.util.Map;

public class RuleContext {

    private String method;
    private Map<String, Object> params = new HashMap<>();
    private Object output;
    private Exception exception;
    private int firedRules;

    public static RuleContext method(String method) {
        RuleContext context = new RuleContext();
        context.setMethod(method);
        return context;
    }

    public RuleContext param(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public int getFiredRules() {
        return firedRules;
    }

    public void setFiredRules(int firedRules) {
        this.firedRules = firedRules;
    }
}
