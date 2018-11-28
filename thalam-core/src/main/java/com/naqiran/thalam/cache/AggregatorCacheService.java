package com.naqiran.thalam.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.constants.ThalamConstants;
import com.naqiran.thalam.service.model.CacheWrapper;
import com.naqiran.thalam.service.model.ServiceException;
import com.naqiran.thalam.service.model.ServiceMessage;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;

/**
 * 
 * @author Nakkeeran Annamalai
 *
 */
public interface AggregatorCacheService {

    public static final Logger log = LoggerFactory.getLogger(AggregatorCacheService.class); 
    
    public default ServiceResponse getValue(final Service service, final ServiceRequest request, Callable<ServiceResponse> remoteCallable) {
        ServiceResponse response = null;
        final String cacheKey = getCacheKey(service, request);
        if (isCached(service) && Boolean.getBoolean(request.getHeaders().get(ThalamConstants.CACHE_OVERRIDE_HEADER))) {
            response = getResponseFromWrapper(getValueFromCache(cacheKey, service.getCacheName()));
        }
        if (response == null) {
            log.info("Cache Miss - Service Id: {} | Cache Name: {} | Cache Key: {}", service.getId(), service.getCacheName(), cacheKey);
            response = getValueFromRemote(service, request, remoteCallable);
            if (isCached(service)) {
                putValueinCache(cacheKey, service.getCacheName(),  getCacheWrapper(response));
            }
        } else {
            log.info("Cache Hit - Service Id: {} | Cache Name: {} | Cache Key: {} | Cached Time:{} | Expiry Time: {}", 
                                            service.getId(), service.getCacheName(), cacheKey, response.getCachedTime(), 
                                            response.getExpiryTime());
        }
        return response;
    }
    
    CacheWrapper getValueFromCache(final String cacheKey, final String cacheName);
    
    void putValueinCache(final String cacheKey, final String cacheName, final CacheWrapper wrapper);
    
    public default boolean isCached(final Service service) {
        return service.isCacheEnabled();
    }
    
    public default String getCacheKey(final Service service, final ServiceRequest request) {
        final String cacheKeyFormat = service.getCacheKeyFormat();
        if (StringUtils.isNotBlank(cacheKeyFormat)) {
            final List<String> keys = Arrays.asList(cacheKeyFormat.split(";"));
            final ExpressionParser parser = new SpelExpressionParser();
            return keys.stream().map(key -> {
                final Expression expression = parser.parseExpression(key);
                final String keyPartial = (String) expression.getValue(request);
                return StringUtils.defaultString(keyPartial);
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
            wrapper.setTtl(response.getTtl());
            if (response.getTtl() > 0) {
                wrapper.setExpiryTime(Instant.now().minusMillis(response.getTtl()));
            }
        }
        return wrapper;
    }
    
    public default ServiceResponse getResponseFromWrapper(final CacheWrapper wrapper) {
        final ServiceResponse serviceResponse = new ServiceResponse();
        if (wrapper != null) {
            final Instant now = Instant.now();
            serviceResponse.setSource("CACHE");
            serviceResponse.setValue(wrapper.getValue());
            serviceResponse.setCurrentTime(now);
            serviceResponse.setCachedTime(wrapper.getCachedTime());
            serviceResponse.setExpiryTime(wrapper.getExpiryTime());
            if (wrapper.getExpiryTime() != null) {
                long millis = Duration.between(now, wrapper.getExpiryTime()).toMillis();
                if (millis > 0) {
                    serviceResponse.setTtl(millis);
                }
            }
            
        } else {
            final ServiceMessage serviceMessage = ServiceMessage.builder().message("Cache Wrapper is empty").build();
            serviceResponse.addMessage(serviceMessage);
        }
        return serviceResponse;
    }
    
    public default ServiceResponse getValueFromRemote(final Service service, final ServiceRequest request, final Callable<ServiceResponse> remoteCallable) {
        try {
            return remoteCallable.call();
        } catch (final Exception e) {
            log.error("Service Id: {} | {}", service.getId(), e.getMessage());
            throw new ServiceException("Error in getting remote response", e);
        }
    }
    
    
    
    static class DefaultAggregatorCacheService implements AggregatorCacheService {
        
        @Autowired
        private AggregatorCoreConfiguration coreConfiguration;
        
        @Autowired
        private CacheManager cacheManager;
        
        @Override
        public boolean isCached(final Service service) {
            return service.isCacheEnabled() && coreConfiguration.getCache().isEnabled();
        }
        
        @Override
        public String getCacheKey(final Service service, final ServiceRequest request) {
            final String[] values = {coreConfiguration.getCache().getCachePrefix(), AggregatorCacheService.super.getCacheKey(service, request)};
            return StringUtils.join(values, "-");
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
            return cache != null ? (CacheWrapper) cache.get(cacheKey) : null;
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