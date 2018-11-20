package com.naqiran.thalam.web;


import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.Map.Entry;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.constants.ThalamConstants;
import com.naqiran.thalam.service.model.ServiceException;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.utils.CoreUtils;

import lombok.Data;
import reactor.core.publisher.Mono;

/**
 * Default Web Client interface for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
public interface AggregatorWebClient {
    
    Mono<ServiceResponse> executeRequest(final Service service, final ServiceRequest request);
    
    UriComponentsBuilder getBaseUrl(final Service service);
    
    default void setCacheHeader(final ServiceResponse serviceResponse, final Service service) {
        long serviceTtl = service.getTtl();
        if (service.getTtlCron() != null) {
            final Date expiryDate = service.getTtlCron().next(new Date());
            serviceTtl = (int) (expiryDate.getTime() - System.currentTimeMillis());
        }
        if (service.isOverrideTTL()) {
            serviceResponse.setCached(true);
            serviceResponse.setTtl(serviceTtl);
        } else if (MapUtils.isNotEmpty(serviceResponse.getHeaders())) {
            final String cacheControlHeader = serviceResponse.getHeaders().get(HttpHeaders.CACHE_CONTROL);
            if (StringUtils.isNotBlank(cacheControlHeader)) {
                Stream.of(cacheControlHeader.split(",")).forEach(str -> {
                    if (str.startsWith("max-age")) {
                        final String[] maxAge = str.split("=");
                        if (maxAge.length > 1) {
                            serviceResponse.setTtl(Integer.valueOf(maxAge[1]) * ThalamConstants.NUMBER_THOUSAND);
                            serviceResponse.setCached(true);
                        } 
                    } 
                });
            }
        } 
    }
    
    /**
     * Create the URL from the Discovery Manager otherwise from the Service Configuration.
     * 
     * @param service
     * @param requestParam
     * @param pathParam
     * @return String
     */
    default URI getURL(final Service service, final ServiceRequest request) {
        final UriComponentsBuilder builder = getBaseUrl(service);
        Map<String,String> paramMap = request.getParameters();
        if (service.isAddAllParam() && MapUtils.isNotEmpty(paramMap)) {
            for (Entry<String, String> entry : paramMap.entrySet()) {
                builder.replaceQueryParam(entry.getKey(), entry.getValue());
            }
        }
        if (MapUtils.isNotEmpty(request.getPathParameters())) {
            paramMap.putAll(request.getPathParameters());
        }
        return builder.buildAndExpand(paramMap).encode().toUri();
    }

    @Data
    public static class DefaultAggregatorWebClient implements AggregatorWebClient {

        @Autowired(required = false)
        private LoadBalancerClient lbClient;
        
        private WebClient client = WebClient.create();
        
        @Override
        public Mono<ServiceResponse> executeRequest(final Service service, final ServiceRequest originalRequest) {
            final ServiceRequest request = CoreUtils.cloneServiceRequestForService(service, originalRequest);
            request.setUri(getURL(service, request));
            client = WebClient.create();
            final HttpMethod requestMethod = Optional.ofNullable(originalRequest.getRequestMethod()).orElse(HttpMethod.GET);
            client.method(requestMethod).uri(request.getUri())
                .exchange().as(resp -> {
                    return resp;
                });
            return null;
        }
        
        @Override
        public UriComponentsBuilder getBaseUrl(final Service service) {
            UriComponentsBuilder builder = null;
            if (StringUtils.isNotBlank(service.getPath())) {
                final String path = StringUtils.substringBefore(service.getPath(), "?");
                final String query = StringUtils.substringAfter(service.getPath(), "?");
                final ServiceInstance instance = lbClient.choose(Optional.ofNullable(service.getDiscoveryId()).orElse(service.getId()));
                if (instance != null) {
                    builder = UriComponentsBuilder.newInstance();
                    builder.host(instance.getHost());
                    if (instance.getPort() > 0) {
                        builder.port(instance.getPort());
                    }
                    final String scheme = service.isSecure() ? "https" : "http";
                    builder.scheme(scheme).path(path).query(query);
                }
                else {
                    Assert.hasText(service.getBaseUrl(), "Base URL should not be empty: " + service.getId());
                    builder = UriComponentsBuilder.fromHttpUrl(service.getBaseUrl()).path(path).query(query);
                }
            }
            else {
                Assert.hasText(service.getBaseUrl(), "Base URL should not be empty: " + service.getId());
                builder = UriComponentsBuilder.fromHttpUrl(service.getBaseUrl());
            }
            return builder;
        }
        
    }
}
