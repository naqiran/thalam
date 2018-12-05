package com.naqiran.thalam.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.naqiran.thalam.cache.AggregatorCacheService;
import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.ExecutionType;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceDictionary;
import com.naqiran.thalam.configuration.ServiceGroup;
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
    
    @Autowired
    private AggregatorCacheService cacheService;
    
    public Mono<ServiceResponse> execute(final String serviceId, final String version, final ServiceRequest request) {
        ServiceGroup serviceGroup = serviceDictionary.getServiceGroupById(serviceId, version);
        
        if (serviceGroup != null) {
            if (ExecutionType.FORK.equals(serviceGroup.getExecutionType())) {
                
            }
        } else {
            final Service service = serviceDictionary.getServiceById(serviceId, version);
            if (service != null) {
                return getResponse(service,request);
            } else {
                
            }
        }
        return Mono.just(null);
    }
    
    public Mono<ServiceResponse> getResponse(final Service service, final ServiceRequest request) {
        boolean isCached = cacheService.isCached(service);
        if (isCached) {
            return cacheService.getValue(service, request, () -> client.executeRequest(service, request));
        } else {
            return client.executeRequest(service, request);
        }
    }
    
    public void wrapCircuitBreaker() {
        
    }
}