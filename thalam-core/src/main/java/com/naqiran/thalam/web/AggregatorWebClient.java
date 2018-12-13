package com.naqiran.thalam.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.Map.Entry;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ClientResponse.Headers;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.constants.ThalamConstants;
import com.naqiran.thalam.service.model.ServiceException;
import com.naqiran.thalam.service.model.ServiceMessage;
import com.naqiran.thalam.service.model.ServiceMessageType;
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
    
    Mono<ServiceResponse> executeRequest(final ServiceRequest request);
    
    default void setCacheHeader(final ServiceResponse serviceResponse, final Service service, final long ttl) {
        Duration serviceTtl = service.getTtl();
        if (service.getTtlCron() != null) {
            final Date expiryDate = service.getTtlCron().next(new Date());
            serviceTtl = Duration.between(Instant.ofEpochMilli(expiryDate.getTime()), Instant.now());
        }
        serviceResponse.setCached(true);
        if (service.isOverrideTTL()) {
            serviceResponse.setTtl(serviceTtl);
        } else if (ttl > 0) {
            serviceResponse.setTtl(Duration.ofSeconds(ttl));
            serviceResponse.setCached(true);
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
    default void decorateServiceURL(final ServiceRequest request) {
        Service service = request.getService();
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
        request.setUri(builder.buildAndExpand(paramMap).encode().toUri());
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
        public Mono<ServiceResponse> executeRequest(final ServiceRequest request) {
            final Service service = request.getService();
            Assert.notNull(service, "Service should not be empty for getting the value from cache");
            decorateServiceURL(request);
            final String url = request.getUri().toString();
            client = WebClient.create();
            final HttpMethod requestMethod = Optional.ofNullable(request.getRequestMethod()).orElse(HttpMethod.GET);
            
            
            Mono<ServiceResponse> monoResponse = client.method(requestMethod).uri(request.getUri()).headers(addHeaders(request)).exchange()
                                            .flatMap(resp -> (!resp.statusCode().isError() ? resp.bodyToMono(service.getResponseType()) : resp.bodyToMono(Map.class))
                                            .map(responseBody -> createServiceResponse(request, responseBody, resp)));
            return monoResponse.doOnError(err -> {
                log.error("Remote Request Error - Service Id: {} | URL: {} | Error: {}" , service.getId(), url, err.getMessage());
            }).onErrorResume(err -> {
                return Mono.error(new ServiceException(err.getMessage()));
            });
        }
        
        public ServiceResponse createServiceResponse(final ServiceRequest request, final Object value, final ClientResponse clientResponse) {
            final Service service = request.getService();
            final String source = request.getUri().toString();
            log.info("Remote Request - Service Id: {} | URL: {}", service.getId(), source);
            final ServiceMessage message = ServiceMessage.builder().id("REMOTE-RESPONSE").message(source).build();
            final ServiceResponse response = ServiceResponse.builder().source(source).build();
            if (!clientResponse.statusCode().isError()) {
                response.setValue(value);
                response.addMessage(message);
                Headers httpHeaders = clientResponse.headers();
                if (httpHeaders != null) {
                    MultiValueMap<String,String> headerMap = httpHeaders.asHttpHeaders();
                    setCacheHeader(response, service, CoreUtils.getCacheControlHeader(headerMap.getFirst(HttpHeaders.CACHE_CONTROL)));
                    Map<String,String> headers = CoreUtils.toSingleValuedMap(headerMap, ThalamConstants.CASE_INSENSITIVE_MAP);
                    response.setHeaders(headers);
                }
            } else if (clientResponse.statusCode().is4xxClientError()){
                message.setMessageBody(value);
                message.setType(ServiceMessageType.ERROR);
                response.addMessage(message);
            } else {
                throw new ServiceException(clientResponse.statusCode().getReasonPhrase());
            }
            return response;
        }
        
        public Consumer<HttpHeaders> addHeaders(final ServiceRequest serviceRequest) {
            return (httpHeaders) -> {
                serviceRequest.getHeaders().entrySet().stream().forEach(entry -> httpHeaders.add(entry.getKey(),entry.getValue()));
            };
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
