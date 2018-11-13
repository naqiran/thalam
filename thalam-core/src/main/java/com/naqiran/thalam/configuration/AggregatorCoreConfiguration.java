package com.naqiran.thalam.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "aggregator")
public class AggregatorCoreConfiguration {
    
    private AggregatorCoreContext context;
    private AggregatorEnvironmentContext environment;
    private WebContext web;
    private ServiceDictionary serviceDictionary;
    private AggregatorCacheContext cache;
    
    @Data
    private static class AggregatorCoreContext {
        private String path;
        private String version;
    }
    
    @Data
    private static class WebContext {
        
    }
    
    @Data
    private static class AggregatorCacheContext {
        private boolean enabled;
        private String cachePrefix;
    }
    
    @Data
    private static class AggregatorEnvironmentContext {
        private String environmentName;
    }
}
