package com.naqiran.thalam.configuration;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;

import com.naqiran.thalam.cache.AggregatorCacheService;
import com.naqiran.thalam.web.AggregatorWebClient;

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
        private List<Attribute> headers;
        private List<Attribute> parameters;
    }
    
    @Data
    public static class AggregatorCacheContext {
        private boolean enabled;
        private String cachePrefix;
        private AggregatorCacheType cacheType;
        
        @Bean
        @ConditionalOnProperty(name = "aggregator.cache.cacheType", havingValue = "CONCURRENT_HASHMAP")
        @ConditionalOnMissingBean(value = AggregatorCacheService.class)
        public AggregatorCacheService createClient(final LoadBalancerClient lbClient) {
            AggregatorCacheService.DefaultAggregatorCacheService cacheService = new AggregatorCacheService.DefaultAggregatorCacheService();
            cacheService.setCacheManager(new ConcurrentMapCacheManager());
            return cacheService;
        }
    }
    
    @Data
    public static class AggregatorEnvironmentContext {
        private String environmentName;
    }
    
    @Bean
    @ConditionalOnMissingBean(value = AggregatorWebClient.class)
    public AggregatorWebClient createClient(final LoadBalancerClient lbClient) {
        AggregatorWebClient.DefaultAggregatorWebClient client = new AggregatorWebClient.DefaultAggregatorWebClient();
        client.setLbClient(lbClient);
        return client;
    }
    
    @Bean
    public static Validator configurationPropertiesValidator() {
        return new ServiceDictionaryValidator();
    }
    
}
