package com.naqiran.thalam.service.model;

import java.time.Instant;

import lombok.Data;

@Data
public class CacheWrapper {
    private String key;
    private Object value;
    private String source;
    private Instant cachedTime;
    private Instant expiryTime;
    private long ttl;
}