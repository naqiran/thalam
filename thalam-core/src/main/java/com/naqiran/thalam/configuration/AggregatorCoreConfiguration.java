package com.naqiran.thalam.configuration;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
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
    private AggregatorCoreContext context = new AggregatorCoreContext();
    private AggregatorEnvironmentContext environment = new AggregatorEnvironmentContext();
    private WebContext web = new WebContext();
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
        private boolean circuitBreakerEnabled = true;
    }
    
    @Data
    public static class AggregatorCacheContext {
        private boolean enabled;
        private String cachePrefix;
        private AggregatorCacheType cacheType;
        
        @Bean("aggregatorCacheClient")
        @ConditionalOnProperty(name = "aggregator.cache.cacheType", havingValue = "CONCURRENT_HASHMAP", matchIfMissing = true)
        @ConditionalOnMissingBean(value = AggregatorCacheService.class)
        public AggregatorCacheService createDefaultCacheService(final LoadBalancerClient lbClient) {
            AggregatorCacheService.DefaultAggregatorCacheService cacheService = new AggregatorCacheService.DefaultAggregatorCacheService();
            cacheService.setCacheManager(new ConcurrentMapCacheManager());
            return cacheService;
        }
    }
    
    @Data
    public static class AggregatorEnvironmentContext {
        private String name;
        private Map<String,String> parameters;
        
        public String getParameters(final String parameterName, final String defaultValue) {
            return MapUtils.isNotEmpty(parameters) ? parameters.getOrDefault(parameterName, defaultValue) : defaultValue;
        }
    }
    
    @Bean
    public static Validator configurationPropertiesValidator() {
        return new ServiceDictionaryValidator();
    }
    
    @Bean
    @ConditionalOnMissingBean(value = AggregatorWebClient.class)
    public AggregatorWebClient createWebClient(final LoadBalancerClient lbClient) {
        AggregatorWebClient.DefaultAggregatorWebClient client = new AggregatorWebClient.DefaultAggregatorWebClient();
        client.setLbClient(lbClient);
        return client;
    }
    
    @Bean
    public ServiceDictionaryBuilder createDictionaryBuilder() {
        return new ServiceDictionaryBuilder();
    }
}
