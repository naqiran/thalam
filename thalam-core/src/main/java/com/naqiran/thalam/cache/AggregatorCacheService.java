package com.naqiran.thalam.cache;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.model.ServiceException;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;

/**
 * 
 * @author Nakkeeran Annamalai
 *
 */
public interface AggregatorCacheService {

    public static final Logger log = LoggerFactory.getLogger(AggregatorCacheService.class); 
    
    public ServiceResponse getValue(final Service service, final ServiceRequest request, Callable<ServiceResponse> response);
    
    public default boolean cacheResponse(final Service service) {
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
        
        @Override
        public boolean cacheResponse(final Service service) {
            return service.isCacheEnabled() && coreConfiguration.getCache().isEnabled();
        }
        
        @Override
        public String getCacheKey(final Service service, final ServiceRequest request) {
            final String[] values = {coreConfiguration.getCache().getCachePrefix(), AggregatorCacheService.super.getCacheKey(service, request)};
            return StringUtils.join(values, ",");
        }

        @Override
        public ServiceResponse getValue(final Service service, final ServiceRequest request, final Callable<ServiceResponse> remoteResponse) {
            ServiceResponse response = null;
            if (cacheResponse(service)) {
                response = null;
            }
            if (response == null) {
                
            }
            return response;
        }
        
        public ServiceResponse getValueFromRemote(final Service service, final ServiceRequest request) {
            return null;
        }
    }
}