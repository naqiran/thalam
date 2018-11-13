package com.naqiran.thalam.service.model;

import java.util.Map;

import lombok.Data;

@Data
public class ServiceRequest {
    private String sequenceId;
    private Map<String,Object> parameters;
    private Map<String,String> headers;
}
