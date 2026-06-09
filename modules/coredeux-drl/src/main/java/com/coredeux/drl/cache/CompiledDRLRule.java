package com.coredeux.drl.cache;

import org.kie.api.KieBase;

/**
 * Cached compiled DRL artifact.
 *
 * <p>This groups the compiled {@link KieBase} together with the metadata that
 * tells the runtime whether the rule source declared the {@code componentRegistry}
 * global.
 *
 * @param kieBase the compiled rule base
 * @param requiresComponentRegistry whether the rule source declares the component registry global
 */
public record CompiledDRLRule(KieBase kieBase, boolean requiresComponentRegistry) {
}
