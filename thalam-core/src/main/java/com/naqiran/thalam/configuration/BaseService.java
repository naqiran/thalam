package com.naqiran.thalam.configuration;

import java.util.function.BiFunction;
import java.util.function.Function;

import javax.validation.constraints.NotBlank;

import com.naqiran.thalam.service.model.ServiceRequest;
import com.naqiran.thalam.service.model.ServiceResponse;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BaseService {
    @NotBlank
    private String id;
    private String version;
    private String description;
    private boolean temp;
    
    private FailureType failureType;
    
    private String sourceExpression;
    private String targetExpression;
    
    private String preValidateExpression;
    private ZipType zipType;
    
    private Function<ServiceRequest, Boolean> validate;
    private Function<ServiceResponse, ServiceResponse> map;
    private BiFunction<ServiceResponse, ServiceResponse, ServiceResponse> zip;
    
    /**
     * Identity Constructor
     */
    public BaseService(final String id) {
        this.id = id;
        this.temp = true;
    }
}
