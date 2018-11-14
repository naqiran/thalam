package com.naqiran.thalam.controller;

import java.time.LocalDateTime;
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

import com.naqiran.thalam.service.ServiceExecutor;
import com.naqiran.thalam.service.model.ServiceMessage;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.utils.CoreUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Data
@Slf4j
@RestController
@RequestMapping("${aggregator.context.path}")
public class ServiceAggregatorController {
    
    @Autowired
    private ServiceExecutor serviceExecutor;
    
    @RequestMapping(value = "/{version}/{serviceId}/{pathParam}")
    public Mono<Object> getResponse(final @PathVariable(name = "version") String version, final @PathVariable(name = "serviceId") String serviceId, 
                                    final @PathVariable(name = "pathParam", required = false) String path, 
                                    final ServerHttpRequest request, final ServerHttpResponse response) {
        log.info("Service Id:{}, Version:{}", serviceId, version);
        final ServiceRequest serviceRequest = CoreUtils.createRequest(request);
        serviceExecutor.execute(serviceRequest);
        return Mono.create(consumer -> {
           consumer.success("First Response"); 
        });
    }
    
    @GetMapping(value = {"/ping", "/"})
    public Mono<Map<String,String>> ping() {
        return Mono.create(consumer -> {
            serviceExecutor.execute(null);
            final Map<String,String> response = new HashMap<>();
            response.put("message", "Ping Message");
            response.put("currentTime", LocalDateTime.now().toString());
            consumer.success(response);
        });
    }
    
    @ExceptionHandler(Exception.class)
    public ServiceResponse errorResponse(final Exception exception) {
        ServiceResponse response = new ServiceResponse();
        ServiceMessage message = new ServiceMessage();
        message.setMessage(exception.getMessage());
        response.addMessage(message);
        return response;
    }
}