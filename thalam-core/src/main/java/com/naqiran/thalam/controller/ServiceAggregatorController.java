package com.naqiran.thalam.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${aggregator.context.path}")
public class ServiceAggregatorController {
    
    @GetMapping("/ping")
    public Mono<Map<String,String>> ping() {
        return Mono.create(subscriber -> {
            final Map<String,String> response = new HashMap<>();
            response.put("message", "Ping Message");
            response.put("currentTime", LocalDateTime.now().toString());
            subscriber.success(response);
        });
    }
    
}