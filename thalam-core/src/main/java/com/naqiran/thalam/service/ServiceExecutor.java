package com.naqiran.thalam.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceDictionary;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.web.AggregatorWebClient;

import lombok.Data;
import reactor.core.publisher.Mono;

/**
 * Thalam (Platform) Executor 
 * @author Nakkeeran Annamalai
 */
@Data
@Component
public class ServiceExecutor {
    
    @Autowired
    private ServiceDictionary serviceDictionary;
    
    @Autowired
    private AggregatorCoreConfiguration configuration;
    
    @Autowired
    private AggregatorWebClient client;
    
    public Mono<ServiceResponse> execute(final String serviceId, final String version, final ServiceRequest request) {
        final Service service = serviceDictionary.getServiceById(serviceId, version);
        Assert.notNull(service, "No Service Exist with the id");
        return getResponse(service,request);
    }
    
    public Mono<ServiceResponse> getResponse(final Service service, final ServiceRequest request) {
        return client.executeRequest(service, request);
    }
    
    public void wrapCircuitBreaker() {
        
    }
}