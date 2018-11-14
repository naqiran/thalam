package com.naqiran.thalam.cache;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;

public interface AggregatorCacheService {
    
    public default ServiceResponse getValueFromCache(final ServiceRequest request) {
        return null;
    }
    
    public default String getCacheKey(final ServiceRequest request, final Service service) {
        final String cacheKeyFormat = service.getCacheKeyFormat();
        final StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(cacheKeyFormat)) {
            if (StringUtils.startsWith(cacheKeyFormat, "service.")) {
                String cacheKey = StringUtils.substringAfter(cacheKeyFormat, "service.");
                return MapUtils.getString(service.getDefaultParameters(), cacheKey);
            }
            final List<String> keys = Arrays.asList(cacheKeyFormat.split("#"));
            
            for (final String key : keys) {
                final ExpressionParser parser = new SpelExpressionParser();
                final Expression expression = parser.parseExpression(key);
                final String keyPartial = (String) expression.getValue(request);
                if (StringUtils.isNotBlank(keyPartial)) {
                    builder.append(keyPartial).append("-");
                }
            }
            if (builder.length() > 1) {
                builder.setLength(builder.length() - 1);
            }
        } else {
            builder.append(request.toString());
        }
        return builder.toString();
    }
    
    
    static class DefaultAggregatorCacheService implements AggregatorCacheService {
        
    }
}