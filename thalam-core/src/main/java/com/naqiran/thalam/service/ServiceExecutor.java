package com.naqiran.thalam.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.naqiran.thalam.cache.AggregatorCacheService;
import com.naqiran.thalam.configuration.ExecutionType;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceDictionary;
import com.naqiran.thalam.configuration.ServiceGroup;
import com.naqiran.thalam.service.model.ServiceException;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.utils.CoreUtils;
import com.naqiran.thalam.web.AggregatorWebClient;

import lombok.Data;
import reactor.core.publisher.Flux;
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
    private AggregatorWebClient client;
    
    @Autowired
    private AggregatorCacheService cacheService;
    
    public Mono<ServiceResponse> execute(final String serviceId, final String version, final ServiceRequest request) {
        ServiceGroup serviceGroup = serviceDictionary.getServiceGroupById(serviceId, version);
        if (serviceGroup != null) {
            return executeGroup(serviceGroup, request);
        } else {
            final Service service = serviceDictionary.getServiceById(serviceId, version);
            if (service != null) {
                return getResponse(service,request);
            } else {
                return Mono.error(() -> new ServiceException("No Service or Service Group Exist with the id :" + serviceId));
            }
        }
    }
    
    public Mono<ServiceResponse> executeGroup(final ServiceGroup serviceGroup, final ServiceRequest request) {
        if (ExecutionType.FORK.equals(serviceGroup.getExecutionType())) {
            final Stream<ServiceRequest> forkedRequests = serviceGroup.getPrepare().apply(request);
            final List<Mono<ServiceResponse>> responses = forkedRequests.map(forkedRequest -> {
                if (serviceGroup.getService() != null) {
                    return getResponse(serviceGroup.getService(), forkedRequest);
                } else if (serviceGroup.getServiceGroup() != null) {
                    return executeGroup(serviceGroup.getServiceGroup(), forkedRequest);
                } 
                return Mono.just(new ServiceResponse());
            }).collect(Collectors.toList());
            return Flux.merge(responses).collectList().flatMap(respList -> {
                return Mono.just(respList.stream().reduce(CoreUtils.createServiceRespone(true), (aggResponse,simpleResponse) -> CoreUtils.aggregateServiceRespone(aggResponse, simpleResponse)));
            });
        } else if (ExecutionType.SERIAL.equals(serviceGroup.getExecutionType())) {
            if (CollectionUtils.isNotEmpty(serviceGroup.getServices())) {
                serviceGroup.getServices().stream().map(svc -> getResponse(svc, request)).reduce((respMono1, respMono2) -> 
                respMono1.zipWith(respMono2, (resp1, resp2) -> resp1));
            }
        }
        return Mono.empty();
    }
    
    public Mono<ServiceResponse> getResponse(final Service service, final ServiceRequest originalRequest) {
        boolean isCached = cacheService.isCached(service);
        final ServiceRequest clonedRequest = CoreUtils.cloneServiceRequestForService(service, originalRequest);
        
        //Prepare the Service Request.
        service.getPrepare().apply(clonedRequest);
        
        //Validate the Service Request.
        boolean isValid = service.getValidate().apply(clonedRequest);
        
        //Map the Service Response after caching.
        if (isValid) {
            if (isCached) {
                return cacheService.getValue(clonedRequest, () -> client.executeRequest(clonedRequest)).map(response -> service.getMap().apply(response));
            } else {
                return client.executeRequest(clonedRequest).map(response -> service.getMap().apply(response));
            }
        }
        return Mono.just(ServiceResponse.builder().source("VALIDATION-FAILED").build());
    }
    
    public void wrapCircuitBreaker() {
        
    }
}