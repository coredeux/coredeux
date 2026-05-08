package com.coredeux.demo.domain.redis;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisSessionSnapshot {

    @Id
    private String id;
    private String username;
    private String state;
    private Instant lastSeenAt;
    private Map<String, Object> attributes = new LinkedHashMap<>();
}
