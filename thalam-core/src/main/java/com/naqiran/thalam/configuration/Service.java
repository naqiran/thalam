package com.naqiran.thalam.configuration;

import java.time.Duration;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;

import org.springframework.http.HttpMethod;
import org.springframework.scheduling.support.CronSequenceGenerator;

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
    @NotBlank
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
    private Duration ttl;
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
        
        if (requestMethod == null) {
            //log.error("Request Method will be defaulted to GET {}", id);
        }
        
        if (!HttpMethod.GET.equals(requestMethod)) {
            if (requestType == null) {
                //log.error("Map type will be defaulted if the request type is empty for service {}", id);
            }
            if (responseType == null) {
                //log.warn("Map type will be defaulted if the response type is empty for service {}", id);
            }
        }
        throw new RuntimeException();
    }
}