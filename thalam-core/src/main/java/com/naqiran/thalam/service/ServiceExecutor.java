package com.naqiran.thalam.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceDictionary;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;

import lombok.Data;
import reactor.core.publisher.Mono;

/**
 * @author Nakkeeran Annamalai
 */
@Data
@Component
public class ServiceExecutor {
    
    @Autowired
    private ServiceDictionary serviceDictionary;
    
    @Autowired
    private AggregatorCoreConfiguration configuration;
    
    public Mono<ServiceResponse> execute(final ServiceRequest request) {
        Service service = serviceDictionary.getServiceById("test-service", "v1");
        Assert.notNull(service, "No Service Exist with the id");
        return null;
    }
    
    public Mono<ServiceResponse> getResponse(final Service service, final ServiceRequest request) {
        if (configuration.getCache().isEnabled()) {
            
        }
        return null;
    }
    
    public void wrapCircuitBreaker() {
        
    }
    
    
}