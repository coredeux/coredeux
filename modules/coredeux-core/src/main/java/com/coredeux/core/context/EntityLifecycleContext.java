package com.coredeux.core.context;

import java.io.Serializable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Captures the framework-resolved entity lifecycle state for a single operation.
 *
 * @param <T> the entity type
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public final class EntityLifecycleContext<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String operation;
    private final Object identifier;
    private final T oldValue;
    private final T newValue;
}
