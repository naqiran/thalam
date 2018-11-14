package com.naqiran.thalam.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;

import com.naqiran.thalam.service.model.ServiceRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CoreUtils {

    public static ServiceRequest createRequest(final ServerHttpRequest request) {
        final ServiceRequest serviceRequest = new ServiceRequest();
        final HttpHeaders headers = request.getHeaders();
        final Map<String,String> headerMap = new HashMap<String,String>();
        if (!CollectionUtils.isEmpty(headers.entrySet())) {
            headers.entrySet().stream().forEach(entry -> {
                if (!CollectionUtils.isEmpty(entry.getValue())) {
                    headerMap.put(entry.getKey(), entry.getValue().get(0));
                }
            });
            
        }
        serviceRequest.setHeaders(headerMap);
        return serviceRequest;
    }
    
}
