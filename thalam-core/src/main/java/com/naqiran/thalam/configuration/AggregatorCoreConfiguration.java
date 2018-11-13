package com.naqiran.thalam.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "aggregator")
public class AggregatorCoreConfiguration {
    
    private AggregatorContext context;
    private WebContext web;
    
    @Data
    private static class AggregatorContext {
        private String path;
    }
    
    @Data
    private static class WebContext {
        
    }
}
