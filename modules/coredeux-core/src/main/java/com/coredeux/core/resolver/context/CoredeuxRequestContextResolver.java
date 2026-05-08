package com.coredeux.core.resolver.context;

import com.coredeux.core.context.RequestContext;

/**
 * Resolves request-scoped framework metadata when a servlet request is present.
 */
public interface CoredeuxRequestContextResolver {

    RequestContext resolve();
}
