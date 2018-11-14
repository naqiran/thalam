package com.naqiran.thalam.service.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

import lombok.Data;

/**
 * Unified Response for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@Data
public class ServiceResponse {
    private Object response;
    private int ttl;
    private String source;
    private LocalDateTime currentTime;
    private List<ServiceMessage> messages;
    
    public void addMessage(ServiceMessage serviceMessage) {
        if (CollectionUtils.isEmpty(messages)) {
            messages = new ArrayList<>();
        }
        messages.add(serviceMessage);
    }
}
