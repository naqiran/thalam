package com.naqiran.thalam.configuration;

import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.validation.constraints.NotBlank;

import org.springframework.http.HttpMethod;
import org.springframework.scheduling.support.CronSequenceGenerator;

import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;

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
    private String version;
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
    private Function<ServiceRequest, Boolean> validate;
    private Function<ServiceResponse, ServiceResponse> map;
    private BiFunction<ServiceResponse, ServiceResponse, ServiceResponse> zip;
    private String preValidateExpression;
    private boolean temp;
    
    /**
     * Identity Constructor
     */
    public Service(final String id) {
        this.id = id;
        this.temp = true;
    }
}