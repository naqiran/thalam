package com.naqiran.thalam.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.naqiran.thalam.cache.AggregatorCacheService;
import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.BaseService;
import com.naqiran.thalam.configuration.ExecutionType;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceDictionary;
import com.naqiran.thalam.configuration.ServiceGroup;
import com.naqiran.thalam.constants.ThalamConstants;
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
    private AggregatorCoreConfiguration configuration;
    
    @Autowired
    private AggregatorWebClient client;
    
    @Autowired
    private AggregatorCacheService cacheService;
    
    public Mono<ServiceResponse> execute(final String serviceId, final String version, final ServiceRequest request) {
        BaseService service = serviceDictionary.getServiceById(serviceId, version);
        return getMonoServiceResponse(service, request, null);
    }
    
    public Mono<ServiceResponse> getMonoServiceResponse(final BaseService service, final ServiceRequest originalRequest, final ServiceResponse previousResponse) {
        if (service instanceof Service) {
            return executeRequest((Service) service, originalRequest, previousResponse);
        } else if (service instanceof ServiceGroup){
            return executeGroupRequest((ServiceGroup)service, originalRequest, previousResponse);
        } else {
            return Mono.error(() -> new ServiceException("No Service or Service Group Exist with the id: " + service.getId()));
        }
    }
    
    public Mono<ServiceResponse> executeGroupRequest(final ServiceGroup serviceGroup, final ServiceRequest request, final ServiceResponse previousResponse) {
        request.setCarriedResponse(previousResponse);
        if (ExecutionType.FORK.equals(serviceGroup.getExecutionType())) {
            final Stream<ServiceRequest> forkedRequests = serviceGroup.getPrepare().apply(request);
            final List<Mono<ServiceResponse>> responses = forkedRequests.map(forkedRequest -> getMonoServiceResponse(serviceGroup.getService(), forkedRequest, null))
                                            .collect(Collectors.toList());
            return Flux.merge(responses).collectList().flatMap(respList -> {
                return Mono.just(respList.stream().reduce(CoreUtils.createServiceResponse(ThalamConstants.FORK_LIST_SOURCE, "Fork Request"), (aggResponse,simpleResponse) -> CoreUtils.aggregateServiceRespone(aggResponse, simpleResponse)));
            });
        } else if (ExecutionType.SERIAL.equals(serviceGroup.getExecutionType())) {
            final ServiceRequest preparedRequest = serviceGroup.getPrepare().apply(request).findFirst().orElse(null);
            final List<BaseService> services = serviceGroup.getServices();
            final String zipType = StringUtils.defaultString(serviceGroup.getZipType(), ThalamConstants.ZIP_DUMMY_SOURCE); 
            Mono<ServiceResponse> monoResponse = CoreUtils.createMonoServiceResponse(zipType, "Dummy ZIP Response");
            if (CollectionUtils.isNotEmpty(services)) {
                for (BaseService service: services) {
                    monoResponse = monoResponse.zipWhen(resp -> getMonoServiceResponse(service,preparedRequest, resp), service.getZip());
                }
                return monoResponse;
            } else {
                return CoreUtils.createMonoServiceResponse(ThalamConstants.SERIAL_ERROR_SOURCE, "Check the serial service group config : " + serviceGroup.getId());
            }
        } else if (ExecutionType.PARALLEL.equals(serviceGroup.getExecutionType())) {
            final ServiceRequest preparedRequest = serviceGroup.getPrepare().apply(request).findFirst().orElse(null);
            final List<BaseService> services = serviceGroup.getServices();
            final String zipType = StringUtils.defaultString(serviceGroup.getZipType(), ThalamConstants.ZIP_DUMMY_SOURCE); 
            Mono<ServiceResponse> monoResponse = CoreUtils.createMonoServiceResponse(zipType, "Dummy ZIP Response");
            if (CollectionUtils.isNotEmpty(services)) {
                for (BaseService service: services) {
                    monoResponse = monoResponse.zipWith(getMonoServiceResponse((Service) service,preparedRequest, null), service.getZip());
                }
                return monoResponse;
            } else {
                return CoreUtils.createMonoServiceResponse(ThalamConstants.PARALLEL_ERROR_SOURCE, "Check the serial service group config : " + serviceGroup.getId());
            }
        }
        return CoreUtils.createMonoServiceResponse(ThalamConstants.NOT_IMPLEMENTED_SOURCE, "Check the serial service group config : " + serviceGroup.getId());
    }
    
    public Mono<ServiceResponse> executeRequest(final Service service, final ServiceRequest originalRequest, final ServiceResponse previousResponse) {
        boolean isCached = cacheService.isCached(service);
        final ServiceRequest clonedRequest = CoreUtils.cloneServiceRequestForService(service, originalRequest, previousResponse);

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
        return CoreUtils.createMonoServiceResponse(ThalamConstants.VALIDATION_FAILED, null);
    }
    
    public void wrapCircuitBreaker() {
        
    }
}