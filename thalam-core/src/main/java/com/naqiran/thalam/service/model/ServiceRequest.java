package com.naqiran.thalam.service.model;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

import com.naqiran.thalam.configuration.Service;

import lombok.Data;

/**
 * Unified Request for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@Data
public class ServiceRequest {
    private String sequenceId;
    private Map<String,String> parameters;
    private Map<String,String> headers;
    private Map<String,String> pathParameters;
    private Map<String,Object> additionalParameters;
    private Object body;
    private URI uri;
    private HttpMethod requestMethod;
    
    @Nullable
    private Service service;
    
    @Nullable
    private ServiceResponse carriedResponse;
}