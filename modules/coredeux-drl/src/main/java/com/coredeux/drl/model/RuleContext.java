package com.coredeux.drl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable execution context passed into DRL rules.
 *
 * <p>The context carries the rule method name, named parameters, arbitrary
 * facts, and rule execution results such as output, exceptions, and fired rule
 * count. Instances are passed by reference into the Drools session so rule
 * consequences can update the same object directly.
 */
public class RuleContext {

    private String method;
    private Map<String, Object> params = new HashMap<>();
    private List<Object> facts = new ArrayList<>();
    private Object output;
    private Exception exception;
    private int firedRules;

    /**
     * Creates a new context with the supplied method name already set.
     *
     * @param method the logical rule method name to match in DRL conditions
     * @return a new {@link RuleContext} instance
     */
    public static RuleContext method(String method) {
        RuleContext context = new RuleContext();
        context.setMethod(method);
        return context;
    }

    /**
     * Adds a named parameter to the context and returns the same instance.
     *
     * <p>Parameters are intended for named inputs such as an entity, request
     * metadata, or other lookup values that rules can access through the
     * context map.
     *
     * @param key the parameter name
     * @param value the parameter value
     * @return this context instance for fluent chaining
     */
    public RuleContext param(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    /**
     * Adds a fact object to the context and returns the same instance.
     *
     * <p>Facts are inserted into the Drools session alongside the context
     * object itself, which allows rules to match on additional domain objects
     * without changing the service API.
     *
     * @param value the fact to add
     * @return this context instance for fluent chaining
     */
    public RuleContext fact(Object value) {
        if (value != null) {
            this.facts.add(value);
        }
        return this;
    }

    /**
     * Returns the logical method name used by the rule condition.
     *
     * @return the context method name, or {@code null} if unset
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the logical method name used by the rule condition.
     *
     * @param method the method name to store in the context
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the live map of named parameters for this context.
     *
     * @return the backing parameter map
     */
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * Replaces the named parameter map used by this context.
     *
     * @param params the new parameter map, or {@code null} to reset to an empty map
     */
    public void setParams(Map<String, Object> params) {
        this.params = params == null ? new HashMap<>() : new HashMap<>(params);
    }

    /**
     * Returns the facts that should be inserted into the Drools session.
     *
     * @return the live list of facts
     */
    public List<Object> getFacts() {
        return facts;
    }

    /**
     * Replaces the facts to be inserted into the Drools session.
     *
     * <p>The supplied list is copied so later external mutation does not change
     * the context after it has been populated.
     *
     * @param facts the new facts, or {@code null} to reset to an empty list
     */
    public void setFacts(List<Object> facts) {
        this.facts = facts == null ? new ArrayList<>() : new ArrayList<>(facts);
    }

    /**
     * Returns the output value populated by a rule consequence.
     *
     * @return the current output, or {@code null} if none has been set
     */
    public Object getOutput() {
        return output;
    }

    /**
     * Sets the output value populated by a rule consequence.
     *
     * @param output the value to expose after rule execution
     */
    public void setOutput(Object output) {
        this.output = output;
    }

    /**
     * Returns the exception captured by a rule consequence, if any.
     *
     * @return the captured exception, or {@code null} if execution completed normally
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Stores an exception captured by a rule consequence.
     *
     * @param exception the exception to expose after rule execution
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }

    /**
     * Returns the number of rules fired during the last execution.
     *
     * @return the number of fired rules
     */
    public int getFiredRules() {
        return firedRules;
    }

    /**
     * Stores the number of rules fired during the last execution.
     *
     * @param firedRules the fired-rule count to record
     */
    public void setFiredRules(int firedRules) {
        this.firedRules = firedRules;
    }
}
