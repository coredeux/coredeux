package com.coredeux.core.handler;

public interface CoredeuxValueHandler<T, V extends ValueContext> {
	
	 T handle(V context);
}