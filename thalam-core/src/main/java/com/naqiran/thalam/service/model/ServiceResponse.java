package com.naqiran.thalam.service.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

import lombok.Data;

/**
 * @author Nakkeeran Annamalai
 */
@Data
public class ServiceResponse {
    private Object response;
    private List<ServiceMessage> messages;
    
    public void addMessage(ServiceMessage serviceMessage) {
        if (CollectionUtils.isEmpty(messages)) {
            messages = new ArrayList<>();
        }
        messages.add(serviceMessage);
    }
}
