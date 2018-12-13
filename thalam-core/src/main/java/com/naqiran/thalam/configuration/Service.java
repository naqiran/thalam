package com.naqiran.thalam.configuration;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.springframework.http.HttpMethod;
import org.springframework.scheduling.support.CronSequenceGenerator;

import com.naqiran.thalam.service.model.ServiceRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Service Description
 * @author Nakkeeran Annamalai
 * 
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class Service extends BaseService {
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
    
    private Function<ServiceRequest,ServiceRequest> prepare;
}