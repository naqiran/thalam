package com.naqiran.thalam.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.naqiran.thalam.cache.AggregatorCacheService;
import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceDictionary;
import com.naqiran.thalam.configuration.ServiceGroup;
import com.naqiran.thalam.service.model.ServiceException;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.utils.CoreUtils;
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
            if(serviceGroup.getPrepare() != null) {
                //final Stream<ServiceRequest> requests = serviceGroup.getPrepare().apply(request);
            }
        } else {
            final Service service = serviceDictionary.getServiceById(serviceId, version);
            if (service != null) {
                return getResponse(service,request);
            } else {
                return Mono.error(() -> new ServiceException("No Service or Service Group Exist with the id :" + serviceId));
            }
        }
        return Mono.error(() -> new ServiceException("Unexpected termination of service look for the logic :" + serviceId));
    }
    
    public Mono<ServiceResponse> getResponse(final Service service, final ServiceRequest originalRequest) {
        boolean isCached = cacheService.isCached(service);
        final ServiceRequest clonedRequest = CoreUtils.cloneServiceRequestForService(service, originalRequest);
        service.getPrepare().apply(clonedRequest);
        boolean isValid = service.getValidate().apply(clonedRequest); 
        if (isValid) {
            if (isCached) {
                return cacheService.getValue(clonedRequest, () -> client.executeRequest(clonedRequest));
            } else {
                return client.executeRequest(clonedRequest);
            }
        }
        return Mono.just(ServiceResponse.builder().source("VALIDATION-FAILED").build());
    }
    
    public void wrapCircuitBreaker() {
        
    }
}