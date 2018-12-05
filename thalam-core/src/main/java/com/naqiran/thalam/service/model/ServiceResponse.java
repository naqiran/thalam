package com.naqiran.thalam.service.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Unified Response for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceResponse {
    private Object value;
    private Map<String,String> headers;
    private boolean cached;
    private Duration ttl;
    private String source;
    
    @Builder.Default
    private final Instant currentTime = Instant.now();
    
    private Instant cachedTime;
    private Instant expiryTime;
    private List<ServiceMessage> messages;
    
    public void addMessage(ServiceMessage serviceMessage) {
        if (CollectionUtils.isEmpty(messages)) {
            messages = new ArrayList<>();
        }
        messages.add(serviceMessage);
    }
}
