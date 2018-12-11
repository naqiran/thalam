package com.naqiran.thalam.configuration;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.naqiran.thalam.service.model.ServiceRequest;

import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ServiceGroup  extends BaseService {
    private ExecutionType executionType;
    private BaseService service;
    private String zipType;
    private List<BaseService> services;
    private String forkAttribute;
    private int maxConcurrent;
    private Function<ServiceRequest, Stream<ServiceRequest>> prepare;
}
