package com.coredeux.drl.core.module;

import com.coredeux.core.module.CoredeuxEntityModuleHandler;

/**
 * Marker contract for entity module handlers implemented as DRL rule sources.
 *
 * <p>These handlers are still registered through the regular
 * {@link CoredeuxEntityModuleHandler} strategy map, but they are expected to be
 * backed by a DRL source rather than a Java bean implementation.
 */
public interface DRLCoredeuxEntityModuleHandler extends CoredeuxEntityModuleHandler {
}
