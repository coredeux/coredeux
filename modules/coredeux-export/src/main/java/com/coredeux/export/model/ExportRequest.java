package com.coredeux.export.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.coredeux.core.search.SearchParams;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String entity;

    @Builder.Default
    private List<ExportField> fieldList = new ArrayList<>();

    @Builder.Default
    private List<SearchParams> searchParams = new ArrayList<>();

    private ExportQuery query;

    @Builder.Default
    private ExportOptions options = ExportOptions.builder().build();
}
