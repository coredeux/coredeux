package com.coredeux.drl.source.resolver;

/**
 * Resolves DRL source text for a rule identifier.
 *
 * <p>Implementations can load rules from the classpath, a database, the file
 * system, object storage, or any other external source.
 */
public interface DRLSourceResolver {

    /**
     * Resolves the DRL source associated with the supplied rule identifier.
     *
     * @param ruleId the rule identifier to resolve
     * @return the DRL source text
     */
    String resolve(String ruleId);
}
