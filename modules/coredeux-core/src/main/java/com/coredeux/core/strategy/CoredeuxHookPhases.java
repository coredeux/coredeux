package com.coredeux.core.strategy;

/**
 * Hook phase keys used in entity definition YAML.
 */
public final class CoredeuxHookPhases {

    public static final String LOAD = "load";
    public static final String BEFORE_SAVE = "before-save";
    public static final String AFTER_SAVE = "after-save";
    public static final String BEFORE_UPDATE = "before-update";
    public static final String AFTER_UPDATE = "after-update";
    public static final String BEFORE_DELETE = "before-delete";
    public static final String BEFORE_REFRESH = "before-refresh";
    public static final String AFTER_REFRESH = "after-refresh";

    private CoredeuxHookPhases() {
    }
}
