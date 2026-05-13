package com.coredeux.demo.domain.elasticsearch;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElasticsearchCatalogEntry {

    private String id;

    private String name;

    private String category;

    private String description;

    private BigDecimal price;
    private List<String> tags = List.of();
    private Map<String, Object> attributes = new LinkedHashMap<>();
}
