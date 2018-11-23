package com.naqiran.thalam.service.model;

import java.time.Instant;

import org.springframework.cache.Cache.ValueWrapper;

import lombok.Data;

@Data
public class CacheWrapper implements ValueWrapper {
    private String key;
    private Object value;
    private String source;
    private Instant cachedTime;
    private Instant expiryTime;
    private long ttl;
    
    @Override
    public Object get() {
        return value;
    }
}