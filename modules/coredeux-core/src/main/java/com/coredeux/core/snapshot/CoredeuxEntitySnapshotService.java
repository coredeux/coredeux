package com.coredeux.core.snapshot;

/**
 * Creates stable lifecycle snapshots for entity values loaded before an
 * operation mutates or persists the incoming entity.
 */
public interface CoredeuxEntitySnapshotService {

    /**
     * Returns a snapshot of the supplied value. Implementations may return the
     * original instance for immutable values.
     *
     * @param value source value
     * @param <T> value type
     * @return snapshot value
     */
    <T> T snapshot(T value);
}
