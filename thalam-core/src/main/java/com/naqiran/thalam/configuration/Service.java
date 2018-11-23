package com.naqiran.thalam.configuration;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import org.springframework.http.HttpMethod;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.util.Assert;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Service Description
 * @author Nakkeeran Annamalai
 * 
 */
@Data
@NoArgsConstructor
public class Service {
    @NotBlank(message = "ID should not be empty")
    private String id;
    private String description;
    private String discoveryId;
    private ServiceType type;
    private HttpMethod requestMethod;
    private String baseUrl;
    private String path;
    private boolean addAllParam;
    private boolean secure;
    private String circuitBreakerId;
    
    private boolean cacheEnabled;
    private String cacheName;
    private String cacheKeyFormat;
    @PositiveOrZero
    private long ttl;
    private boolean overrideTTL;
    private CronSequenceGenerator ttlCron;
    private String ttlExpression;
    
    private List<Attribute> headers;
    private List<Attribute> parameters;
    private Class<?> responseType;
    private Class<?> requestType;
    
    @PostConstruct
    public void servicePostConfiguration() {
        if (CronSequenceGenerator.isValidExpression(ttlExpression)) {
            ttlCron = new CronSequenceGenerator(ttlExpression);
        }
        
        if (cacheEnabled) {
            Assert.hasText(cacheName, "Cache Name should not be empty");
            Assert.hasText(cacheKeyFormat, "Cache Key format should not be empty when cache is enabled");
        }
        
    }
}