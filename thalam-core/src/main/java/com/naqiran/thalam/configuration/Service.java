package com.naqiran.thalam.configuration;

import java.lang.reflect.Type;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import org.springframework.http.HttpMethod;

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
    private ServiceType type = ServiceType.WEB;
    private HttpMethod requestMethod;
    private String baseUrl;
    private String path;
    private boolean addAllParam;
    private boolean secure;
    private String circuitBreakerId;
    private boolean cacheEnabled;
    private String cacheKeyFormat;
    @PositiveOrZero
    private int ttl;
    private boolean overrideTTL;
    private String sourceParameter;
    private String targetParameter;
    private List<Attribute> headers;
    private List<Attribute> parameters;
    private Class<?> responseType;
}