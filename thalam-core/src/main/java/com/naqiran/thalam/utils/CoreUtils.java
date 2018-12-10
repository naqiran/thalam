package com.naqiran.thalam.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
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
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.Attribute;
import com.naqiran.thalam.configuration.AttributeType;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceGroup;
import com.naqiran.thalam.constants.ThalamConstants;
import com.naqiran.thalam.service.model.ServiceMessage;
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
            clonedRequest.setService(service);
            clonedRequest.setHeaders(new CaseInsensitiveMap<>(serviceRequest.getHeaders()));
            clonedRequest.setParameters(new HashMap<>(serviceRequest.getParameters()));
            clonedRequest.setBody(serviceRequest.getBody());
            clonedRequest.setRequestMethod(serviceRequest.getRequestMethod());
            CoreUtils.decorateAttributes(serviceRequest.getParameters(), service.getParameters());
            CoreUtils.decorateAttributes(serviceRequest.getHeaders(), service.getHeaders());
        }
        return clonedRequest;
    }
    
    public static ServiceRequest cloneServiceRequestForServiceGroup(final ServiceGroup group, final ServiceRequest serviceRequest) {
        final ServiceRequest clonedRequest = new ServiceRequest();
        clonedRequest.setHeaders(new CaseInsensitiveMap<>(serviceRequest.getHeaders()));
        clonedRequest.setParameters(new HashMap<>(serviceRequest.getParameters()));
        clonedRequest.setBody(serviceRequest.getBody());
        clonedRequest.setRequestMethod(serviceRequest.getRequestMethod());
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
     * To Mono Response
     * @param request
     * @param response
     * @param serverRequest
     * @param serverResponse
     */
    public static Mono<?> toResponse(final ServiceRequest request, final Mono<ServiceResponse> response, final ServerHttpRequest serverRequest, final ServerHttpResponse serverResponse) {
        return response.map(resp -> {
            if (request.getHeaders().containsKey(ThalamConstants.DEBUG_HEADER) || resp.getValue() == null) {
                return resp;
            }
            return resp.getValue();
        });
    }
    
    /**
     * Evaluate the Spring Expression Language.
     * @param expressionString
     * @param request
     * @return String
     */
    public static <T> T evaluateSPEL(final String expressionString, final ServiceRequest request, Class<T> clazz) {
        try {
            final ExpressionParser parser = new SpelExpressionParser();
            final Expression expression = parser.parseExpression(expressionString);
            return expression.getValue(request, clazz);
        } catch (final Exception e) {
            log.error("Error Evaluating Expression: {}", e.getMessage());
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public static ServiceResponse aggregateServiceRespone(final ServiceResponse aggregatedResponse, final ServiceResponse simpleResponse) {
        if (simpleResponse.getValue() != null) {
            ((ArrayList<Object>)aggregatedResponse.getValue()).add(simpleResponse.getValue());
        }
        if (CollectionUtils.isNotEmpty(simpleResponse.getMessages())) {
            aggregatedResponse.getMessages().addAll(simpleResponse.getMessages());
        }
        return aggregatedResponse;
    }
    
    public static Mono<ServiceResponse> createMonoServiceResponse(final String source, final String message) {
        return Mono.just(createServiceResponse(source,message));
    }
    
    public static ServiceResponse createServiceResponse(final String source, final String message) {
        ServiceResponse response = ServiceResponse.builder()
                                        .source(StringUtils.defaultString(source, ThalamConstants.DUMMY_SOURCE)).build();
        if (StringUtils.isNotBlank(message)) {
            response.addMessage(ServiceMessage.builder().message(message).build());
        }
        if (ThalamConstants.ZIP_MAP_SOURCE.equals(source)) {
            response.setValue(new HashMap<String,Object>());
        } else if (ThalamConstants.FORK_LIST_SOURCE.equals(source)) {
            response.setValue(new ArrayList<>());
        }
        return response;
    }
    
    public static void validateLifeCycleFunction(final Method method, final String message, final Class<?> responseType, Class<?>... methodParameters) {
        Assert.state(method.getReturnType().isAssignableFrom(responseType), message);
        final Parameter[] parameters = method.getParameters();
        if (parameters != null && methodParameters != null) {
            Assert.state(parameters.length == methodParameters.length, message);
            for (int i=0; i < parameters.length; i++) {
                Assert.state(parameters[i].getType().equals(methodParameters[i]), message);
            }
        }
    }
}
