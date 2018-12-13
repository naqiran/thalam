package com.naqiran.thalam.service.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.naqiran.thalam.configuration.FailureType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified Response for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceResponse {
    private Object value;
    private Map<String,String> headers;
    private boolean cached;
    private Duration ttl;
    private String source;
    private FailureType failureType;
    
    @Builder.Default
    private final Instant currentTime = Instant.now();
    
    private Instant cachedTime;
    private Instant expiryTime;
    private List<ServiceMessage> messages;
    
    public void addMessage(ServiceMessage serviceMessage) {
        if (serviceMessage != null) {
            if (CollectionUtils.isEmpty(messages)) {
                messages = new ArrayList<>();
            }
            messages.add(serviceMessage);
        }
    }
}
