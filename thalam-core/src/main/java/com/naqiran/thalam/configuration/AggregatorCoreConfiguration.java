package com.naqiran.thalam.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Core Configuration for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aggregator")
public class AggregatorCoreConfiguration {
    private AggregatorCoreContext context;
    private AggregatorEnvironmentContext environment;
    private WebContext web;
    private AggregatorCacheContext cache = new AggregatorCacheContext();
    
    @Data
    public static class AggregatorCoreContext {
        private String path;
        private String version;
    }
    
    @Data
    public static class WebContext {
        
    }
    
    @Data
    public static class AggregatorCacheContext {
        private boolean enabled;
        private String cachePrefix;
        private AggregatorCacheType cacheType;
    }
    
    @Data
    public static class AggregatorEnvironmentContext {
        private String environmentName;
    }
}
