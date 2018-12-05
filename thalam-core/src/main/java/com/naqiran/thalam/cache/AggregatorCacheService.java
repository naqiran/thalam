package com.naqiran.thalam.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.constants.ThalamConstants;
import com.naqiran.thalam.service.model.CacheWrapper;
import com.naqiran.thalam.service.model.ServiceMessage;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;
import com.naqiran.thalam.utils.CoreUtils;

import reactor.core.publisher.Mono;

/**
 * 
 * @author Nakkeeran Annamalai
 *
 */
public interface AggregatorCacheService {

    public static final Logger log = LoggerFactory.getLogger(AggregatorCacheService.class); 
    
    @Nullable CacheWrapper getValueFromCache(final String cacheKey, final String cacheName);
    
    void putValueinCache(final String cacheKey, final String cacheName, final CacheWrapper wrapper);
    
    public default Mono<ServiceResponse> getValue(final Service service, final ServiceRequest request, Supplier<Mono<ServiceResponse>> remoteSupplier) {
        ServiceResponse response = null;
        final String cacheKey = getCacheKey(service, request);
        final boolean cached = isCached(service);
        if (cached && !request.getHeaders().containsKey(ThalamConstants.CACHE_OVERRIDE_HEADER)) {
            response = getResponseFromWrapper(cacheKey, getValueFromCache(cacheKey, service.getCacheName()));
        }
        if (response == null) {
            log.info("Cache Miss - Service Id: {} | Cache Name: {} | Cache Key: {}", service.getId(), service.getCacheName(), cacheKey);
            Mono<ServiceResponse> monoResponse = remoteSupplier.get().doOnSuccess(serviceResponse -> {
                if (serviceResponse != null && serviceResponse.getValue() != null && cached) {
                    putValueinCache(cacheKey, service.getCacheName(),  getCacheWrapper(serviceResponse));
                }
            });
            return monoResponse;
        } else {
            log.info("Cache Hit - Service Id: {} | Cache Name: {} | Cache Key: {} | Cached Time:{} | Expiry Time: {}", 
                                            service.getId(), service.getCacheName(), cacheKey, response.getCachedTime(), 
                                            response.getExpiryTime());
            return Mono.just(response);
        }
    }
    
    public default boolean isCached(final Service service) {
        return service.isCacheEnabled();
    }
    
    public default String getCacheKey(final Service service, final ServiceRequest request) {
        final String cacheKeyFormat = service.getCacheKeyFormat();
        if (StringUtils.isNotBlank(cacheKeyFormat)) {
            final List<String> keys = Arrays.asList(cacheKeyFormat.split(";"));
            return keys.stream().map(key -> {
                return CoreUtils.evaluateSPEL(key, request);
            }).collect(Collectors.joining("-"));
        }
        return service.getId();
    }
    
    public default CacheWrapper getCacheWrapper(final ServiceResponse response) {
        CacheWrapper wrapper = null;
        if (response != null) {
            wrapper = new CacheWrapper();
            wrapper.setValue(response.getValue());
            wrapper.setSource(response.getSource());
            wrapper.setCachedTime(Instant.now());
            if (response.getTtl() != null) {
                wrapper.setTtl(response.getTtl().toMillis());
                wrapper.setExpiryTime(Instant.now().minus(response.getTtl()));
            }
        }
        return wrapper;
    }
    
    public default ServiceResponse getResponseFromWrapper(final String cacheKey, final CacheWrapper wrapper) {
        ServiceResponse serviceResponse = null;
        if (wrapper != null) {
            final ServiceMessage message = ServiceMessage.builder().id("CACHED-RESPONSE").message(cacheKey).build();
            serviceResponse = ServiceResponse.builder()
                                            .source(ThalamConstants.CACHE)
                                            .value(wrapper.getValue())
                                            .cachedTime(wrapper.getCachedTime())
                                            .expiryTime(wrapper.getExpiryTime())
                                            .ttl(wrapper.getExpiryTime() != null ? Duration.between(Instant.now(), wrapper.getExpiryTime()): null)
                                            .build();
            serviceResponse.addMessage(message);
        } 
        return serviceResponse;
    }
    
    static class DefaultAggregatorCacheService implements AggregatorCacheService {
        
        @Autowired
        private AggregatorCoreConfiguration coreConfiguration;
        
        private CacheManager cacheManager;
        
        @Override
        public boolean isCached(final Service service) {
            return service.isCacheEnabled() && coreConfiguration.getCache().isEnabled();
        }
        
        @Override
        public String getCacheKey(final Service service, final ServiceRequest request) {
            final String prefix = coreConfiguration.getCache().getCachePrefix();
            final String cacheKey = AggregatorCacheService.super.getCacheKey(service, request);
            return StringUtils.isNotBlank(prefix) ? prefix + "-" + cacheKey : cacheKey;
        }

        public Cache getCache(final String cacheName) {
            return cacheManager.getCache(cacheName);
        }

        public void setCacheManager(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }

        @Override
        public @Nullable CacheWrapper getValueFromCache(String cacheKey, final String cacheName) {
            Cache cache = getCache(cacheName);
            return cache != null ? cache.get(cacheKey, CacheWrapper.class) : null;
        }

        @Override
        public void putValueinCache(String cacheKey, final String cacheName, CacheWrapper response) {
            final Cache cache = getCache(cacheName);
            if (cache != null) {
                cache.put(cacheKey, response);
            }
        }
    }
}