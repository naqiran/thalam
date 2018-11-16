package com.naqiran.thalam.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.util.MultiValueMap;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.Attribute;
import com.naqiran.thalam.configuration.AttributeType;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.service.model.ServiceRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Core Utility for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CoreUtils {

    /**
     * Create the Service Request from the HTTP Request
     * @param request
     * @param configuration
     * @return ServiceRequest
     */
    public static ServiceRequest createServiceRequest(final ServerHttpRequest request, final AggregatorCoreConfiguration configuration) {
        final ServiceRequest serviceRequest = new ServiceRequest();
        final Map<String,String> headerMap = CoreUtils.toSingleValuedMap(request.getHeaders());
        serviceRequest.setHeaders(headerMap);
        
        final Map<String,String> parametersMap = CoreUtils.toSingleValuedMap(request.getQueryParams());
        serviceRequest.setParameters(parametersMap);
        
        //Decorate the default value from the configuration.
        if (configuration != null && configuration.getWeb() != null) {
            CoreUtils.decorateAttributes(headerMap, configuration.getWeb().getHeaders());
            CoreUtils.decorateAttributes(parametersMap, configuration.getWeb().getParameters());
        }
        
        return serviceRequest;
    }
    
    /**
     * 
     * @param service
     * @param serviceRequest
     * @return ServiceRequest
     */
    public static ServiceRequest cloneServiceRequestForService(final Service service, final ServiceRequest serviceRequest) {
        final ServiceRequest clonedRequest = new ServiceRequest();
        if (serviceRequest != null) {
            clonedRequest.setHeaders(new HashMap<>(serviceRequest.getHeaders()));
            clonedRequest.setParameters(new HashMap<>(serviceRequest.getParameters()));
            clonedRequest.setBody(serviceRequest.getBody());
            CoreUtils.decorateAttributes(serviceRequest.getParameters(), service.getParameters());
            CoreUtils.decorateAttributes(serviceRequest.getHeaders(), service.getHeaders());
        }
        return clonedRequest;
    }
    
    /**
     * Decorate the Map with the default attributes.
     * @param attributeMap
     * @param attributes
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String,T> decorateAttributes(final Map<String,T> attributeMap, final List<Attribute> attributes) {
        if (CollectionUtils.isNotEmpty(attributes) && attributeMap != null) {
            attributes.stream().forEach(attribute -> {
                if (AttributeType.OVERRIDE.equals(attribute.getType())) {
                    attributeMap.put(attribute.getName(), (T) attribute.getValue());
                } else if (AttributeType.STRIP.equals(attribute.getType())) {
                    attributeMap.remove(attribute.getName());
                } else if (AttributeType.DEFAULT.equals(attribute.getType())){
                    attributeMap.putIfAbsent(attribute.getName(), (T) attribute.getValue());
                }
            });
        }
        return attributeMap;
    }
    
    /**
     * Creates Mutable Empty Single Valued Map.
     * @param mvMap
     * @return Map
     */
    public static <K,V> Map<K,V> toSingleValuedMap(final MultiValueMap<K,V> mvMap) {
        final Map<K,V> singleValuedMap = new HashMap<K,V>();
        if (MapUtils.isNotEmpty(mvMap)) {
            mvMap.forEach((key,value) -> singleValuedMap.put(key,value.get(0)));
        }
        return singleValuedMap;
    }
    
}
