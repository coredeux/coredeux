package com.coredeux.core.handler.service;

import com.coredeux.core.handler.ValueContext;

public interface CoredeuxValueHandlerService {

	<T, V extends ValueContext> T invoke(String handler, V context);
}
