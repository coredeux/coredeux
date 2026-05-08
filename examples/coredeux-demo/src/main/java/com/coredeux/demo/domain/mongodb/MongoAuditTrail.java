package com.coredeux.demo.domain.mongodb;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "coredeux_demo_mongo_audit_trail")
public class MongoAuditTrail {

    @Id
    private String id;
    private String source;
    private String category;
    private Instant occurredAt;
    private Map<String, Object> details = new LinkedHashMap<>();
}
