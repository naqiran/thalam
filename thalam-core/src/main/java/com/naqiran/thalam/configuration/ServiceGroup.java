package com.naqiran.thalam.configuration;

import java.util.List;

import javax.validation.constraints.NotBlank;

import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@NoArgsConstructor
public class ServiceGroup {
    @NotBlank
    private String id;
    private String description;
    private ExecutionType executionType;
    private Service service;
    private ServiceGroup serviceGroup;
    private List<Service> services;
    private List<ServiceGroup> serviceGroups;
}
