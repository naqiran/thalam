package com.naqiran.thalam.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.service.ServiceExecutor;
import com.naqiran.thalam.service.model.ServiceMessage;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.utils.CoreUtils;

import lombok.Data;
import reactor.core.publisher.Mono;

/**
 * Unified Controller for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@Data
@RestController
@RequestMapping("${aggregator.context.path}")
public class ServiceAggregatorController {
    
    @Autowired
    private AggregatorCoreConfiguration configuration;
    
    @Autowired
    private ServiceExecutor serviceExecutor;
    
    @RequestMapping(value = "/{version}/{serviceId}")
    public Mono<?> getResponse(final @PathVariable(name = "version") String version, final @PathVariable(name = "serviceId") String serviceId, 
                                    final ServerHttpRequest serverRequest, final ServerHttpResponse serverResponse) {
        final ServiceRequest serviceRequest = CoreUtils.createServiceRequest(serverRequest,configuration);
        Mono<ServiceResponse> serviceResponse = serviceExecutor.execute(serviceId, version, serviceRequest);
        return CoreUtils.toResponse(serviceRequest, serviceResponse, serverRequest, serverResponse);
    }
    
    @GetMapping(value = {"/ping", "/"})
    public Mono<Map<String,String>> ping() {
        return Mono.create(consumer -> {
            final Map<String,String> response = new HashMap<>();
            response.put("message", "Ping Message");
            response.put("currentTime", Instant.now().toString());
            consumer.success(response);
        });
    }
    
    @ExceptionHandler(Exception.class)
    public ServiceResponse errorResponse(final Exception exception) {
        final ServiceResponse response = new ServiceResponse();
        response.setCurrentTime(Instant.now());
        final ServiceMessage message = new ServiceMessage();
        message.setMessage(exception.getMessage());
        response.addMessage(message);
        return response;
    }
}