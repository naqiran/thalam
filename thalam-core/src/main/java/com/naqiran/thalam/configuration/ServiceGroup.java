package com.naqiran.thalam.configuration;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.validation.constraints.NotBlank;

import com.naqiran.thalam.service.model.ServiceRequest;

import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@NoArgsConstructor
public class ServiceGroup {
    @NotBlank
    private String id;
    private String version;
    private String description;
    private ExecutionType executionType;
    private Service service;
    private ServiceGroup serviceGroup;
    private List<Service> services;
    private List<ServiceGroup> serviceGroups;
    private String forkAttribute;
    private int maxConcurrent;
    private Function<ServiceRequest, Stream<ServiceRequest>> prepare;
}
