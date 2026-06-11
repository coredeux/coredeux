package com.coredeux.drl.core.handler;

import com.coredeux.drl.model.RuleContext;

public interface DRLCoredeuxValueHandler {
	
	<T> void handle(RuleContext<T> $context);
	
}
