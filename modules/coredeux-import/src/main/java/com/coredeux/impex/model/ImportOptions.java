package com.coredeux.impex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportOptions {

    /**
     * Number of execution passes. Multiple passes allow forward references to be
     * resolved after later rows create or fetch them.
     */
    @Builder.Default
    private int passes = 2;

    /**
     * Stops execution after the first final-pass row error when true.
     */
    private boolean failFast;

    /**
     * Indicates that callers intend validation-only processing. The current service
     * exposes validateData separately; this flag is retained for JSON contract
     * completeness.
     */
    private boolean validateOnly;
}
