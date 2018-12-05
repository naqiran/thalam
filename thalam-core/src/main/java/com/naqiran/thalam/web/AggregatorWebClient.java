package com.naqiran.thalam.web;


import java.net.URI;
import java.time.Duration;
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
import com.naqiran.thalam.service.model.ServiceMessage;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.utils.CoreUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Default Web Client interface for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@FunctionalInterface
public interface AggregatorWebClient {
    
    Mono<ServiceResponse> executeRequest(final Service service, final ServiceRequest request);
    
    default void setCacheHeader(final ServiceResponse serviceResponse, final Service service) {
        Duration serviceTtl = service.getTtl();
        if (service.getTtlCron() != null) {
            final Date expiryDate = service.getTtlCron().next(new Date());
            serviceTtl = Duration.between(Instant.ofEpochMilli(expiryDate.getTime()), Instant.now());
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
                            serviceResponse.setTtl(Duration.ofSeconds(Long.valueOf(maxAge[0])));
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
    
    default UriComponentsBuilder getBaseUrl(final Service service) {
        Assert.hasText(service.getBaseUrl(), "Base URL should not be empty: " + service.getId());
        return UriComponentsBuilder.fromHttpUrl(service.getBaseUrl())
                                        .path(StringUtils.substringBefore(service.getPath(), "?"))
                                        .scheme(service.isSecure() ? "https" : "http")
                                        .query(StringUtils.substringAfter(service.getPath(), "?"));
    }

    @Slf4j
    @Data
    public static class DefaultAggregatorWebClient implements AggregatorWebClient {

        @Autowired(required = false)
        private LoadBalancerClient lbClient;
        
        private WebClient client = WebClient.create();
        
        @Override
        public Mono<ServiceResponse> executeRequest(final Service service, final ServiceRequest originalRequest) {
            final ServiceRequest request = CoreUtils.cloneServiceRequestForService(service, originalRequest);
            final URI uri = getURL(service, request);
            request.setUri(uri);
            final String url = uri.toString();
            client = WebClient.create();
            final HttpMethod requestMethod = Optional.ofNullable(originalRequest.getRequestMethod()).orElse(HttpMethod.GET);
            Mono<?> monoResponse = client.method(requestMethod).uri(request.getUri()).retrieve().bodyToMono(service.getResponseType());
            long startTime = System.nanoTime();
            return monoResponse.map(resp -> {
                log.info("Remote Request - Service Id: {} | URL: {} | Time Taken: {}", service.getId(), url, System.nanoTime() - startTime);
                final ServiceMessage message = ServiceMessage.builder().id("REMOTE-RESPONSE").message(url).build();
                final ServiceResponse response = ServiceResponse.builder().source(url).value(resp).build();
                response.addMessage(message);
                return response;
            }).doOnError(err -> {
                log.error("Remote Request Error - Service Id: {} | URL: {} | Error: {}" , service.getId(), url, err.getMessage());
            }).onErrorResume(err -> {
                final ServiceResponse errorResponse = ServiceResponse.builder().source(url).build();
                errorResponse.addMessage(ServiceMessage.builder().message(err.getMessage()).build());
                return Mono.just(errorResponse);
            });
        }
        
        @Override
        public UriComponentsBuilder getBaseUrl(final Service service) {
            UriComponentsBuilder builder = AggregatorWebClient.super.getBaseUrl(service);
            if (StringUtils.isNotBlank(service.getPath())) {
                final ServiceInstance instance = lbClient.choose(Optional.ofNullable(service.getDiscoveryId()).orElse(service.getId()));
                if (instance != null) {
                    builder.host(instance.getHost());
                    if (instance.getPort() > 0) {
                        builder.port(instance.getPort());
                    }
                }
            }
            return builder;
        }
        
    }
}
