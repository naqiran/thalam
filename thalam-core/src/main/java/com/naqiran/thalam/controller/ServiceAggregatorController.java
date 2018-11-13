package com.naqiran.thalam.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("${aggregator.context.path}")
public class ServiceAggregatorController {
    
    @RequestMapping(value = "/{version}/{serviceId}/{pathParam}")
    public Mono<Object> getResponse(final @RequestParam(name = "version") String version, final @RequestParam(name = "serviceId") String serviceId, 
                                    final @RequestParam(name = "pathParam", required = false) String path, 
                                    final ServerHttpRequest request, final ServerHttpResponse response) {
        log.info("Service Id:{}, Version:{}", serviceId, version);
        return Mono.create(consumer -> {
           consumer.success("First Response"); 
        });
    }
    
    @GetMapping(value = {"/ping", "/"})
    public Mono<Map<String,String>> ping() {
        return Mono.create(consumer -> {
            final Map<String,String> response = new HashMap<>();
            response.put("message", "Ping Message");
            response.put("currentTime", LocalDateTime.now().toString());
            consumer.success(response);
        });
    }
    
}