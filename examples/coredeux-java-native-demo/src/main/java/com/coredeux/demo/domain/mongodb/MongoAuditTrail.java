package com.coredeux.demo.domain.mongodb;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MongoAuditTrail {

    private String id;
    private String source;
    private String category;
    private Instant occurredAt;
    private Map<String, Object> details = new LinkedHashMap<>();
}
