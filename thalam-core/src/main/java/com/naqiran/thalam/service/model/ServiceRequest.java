package com.naqiran.thalam.service.model;

import java.util.Map;

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
}
