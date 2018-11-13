package com.naqiran.thalam.service;

import org.springframework.stereotype.Component;

import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;

import reactor.core.publisher.Mono;

/**
 * @author Nakkeeran Annamalai
 */
@Component
public class ServiceExecutor {
    public Mono<ServiceResponse> execute(final ServiceRequest request) {
        return null;
    }
}