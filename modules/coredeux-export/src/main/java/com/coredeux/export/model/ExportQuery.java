package com.coredeux.export.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private String text;

    @Builder.Default
    private Map<String, Object> params = new LinkedHashMap<>();
}
