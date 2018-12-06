package com.naqiran.thalam.sample.application;

import org.springframework.stereotype.Component;

import com.naqiran.thalam.annotations.AggregatingFunctors;
import com.naqiran.thalam.annotations.Prepare;
import com.naqiran.thalam.annotations.Validate;
import com.naqiran.thalam.service.model.ServiceRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@AggregatingFunctors
@Component
public class AggregatingServiceFunctors {

    @Prepare(service = "pet-service1", version = "v1")
    public ServiceRequest prepare(final ServiceRequest request) {
        log.error("This is the prepare method for pet service");
        return request;
    }
    
    @Validate(service = "pet-service1", version = "v1")
    public Boolean validate(final ServiceRequest request) {
        log.error("This is the validate method for pet service");
        return true;
    }
}
