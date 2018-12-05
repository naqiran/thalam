package com.naqiran.thalam.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.Attribute;
import com.naqiran.thalam.configuration.AttributeType;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.constants.ThalamConstants;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Core Utility for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@Slf4j
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
        final Map<String,String> headerMap = CoreUtils.toSingleValuedMap(request.getHeaders(), ThalamConstants.CASE_INSENSITIVE_MAP);
        serviceRequest.setHeaders(headerMap);
        
        final Map<String,String> parametersMap = CoreUtils.toSingleValuedMap(request.getQueryParams(), null);
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
            clonedRequest.setRequestMethod(serviceRequest.getRequestMethod());
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
    public static <K,V> Map<K,V> toSingleValuedMap(final MultiValueMap<K,V> mvMap, String type) {
        final Map<K,V> singleValuedMap = ThalamConstants.CASE_INSENSITIVE_MAP.equals(type) ? new CaseInsensitiveMap<K,V>() : new HashMap<K,V>();
        if (MapUtils.isNotEmpty(mvMap)) {
            mvMap.forEach((key,value) -> singleValuedMap.put(key,value.get(0)));
        }
        return singleValuedMap;
    }
    
    /**
     * 
     */
    public static Mono<?> toResponse(final ServiceRequest request, final Mono<ServiceResponse> response, final ServerHttpRequest serverRequest, final ServerHttpResponse serverResponse) {
        return response.map(resp -> {
            if (request.getHeaders().containsKey(ThalamConstants.DEBUG_HEADER)) {
                return resp;
            }
            return resp.getValue();
        });
    }
    
    public static String evaluateSPEL(final String expressionString, final ServiceRequest request) {
        String keyPartial = null;
        try {
            final ExpressionParser parser = new SpelExpressionParser();
            final Expression expression = parser.parseExpression(expressionString);
            keyPartial = (String) expression.getValue(request);
        } catch (final Exception e) {
            log.error("Error Evaluating Expression: {}", e.getMessage());
        }
        return StringUtils.defaultString(keyPartial);
    }
}
