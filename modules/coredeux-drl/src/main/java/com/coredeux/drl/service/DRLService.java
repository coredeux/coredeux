package com.coredeux.drl.service;

import com.coredeux.drl.model.RuleContext;

public interface DRLService {

    void execute(String ruleId, RuleContext context);

    void execute(String ruleId, RuleContext context, Object... facts);

    void purgeCache();

    void purgeCache(String ruleId);

    boolean isCached(String ruleId);
}
