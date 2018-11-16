package com.naqiran.thalam.web;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.utils.CoreUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Default Web Client interface for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
public interface AggregatorWebClient {
    
    ServiceResponse executeRequest(final Service service, final ServiceRequest request);
    
    UriComponentsBuilder getBaseUrl(final Service service);
    
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

    @Slf4j
    public class DefaultAggregatorWebClient implements AggregatorWebClient {

        @Autowired
        private LoadBalancerClient lbClient;
        
        @Override
        public ServiceResponse executeRequest(final Service service, final ServiceRequest originalRequest) {
            final ServiceRequest request = CoreUtils.cloneServiceRequestForService(service, originalRequest);
            request.setUri(getURL(service, request));
            ServiceResponse response = new ServiceResponse();
            response.setCurrentTime(Instant.now());
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
