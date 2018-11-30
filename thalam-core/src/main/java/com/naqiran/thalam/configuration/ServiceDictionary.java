package com.naqiran.thalam.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Service Dictionary Configuration.
 * @author Nakkeeran Annamalai
 *
 */
@Data
@Slf4j
@Validated
@Configuration
@ConfigurationProperties(prefix = "aggregator.dictionary")
public class ServiceDictionary {
    
    private String name;
    
    @Valid
    private List<Service> services;
    
    @Valid
    private List<ServiceGroup> serviceGroups;
    private Map<String,Service> serviceMap;
    private Map<String,ServiceGroup> serviceGroupMap;
    
    @PostConstruct
    private void buildServices() {
        log.info("Initializing the Service Dictionary {}", name);
        if (!CollectionUtils.isEmpty(services)) {
            log.info("******************** Services ******************************");
            serviceMap = services.stream().peek(service -> log.info("{}", service))
                                            .collect(Collectors.toMap(Service::getId, Function.identity()));
        } else {
            serviceMap = new HashMap<>();
            log.warn("No Services Configured");
        }
        if (!CollectionUtils.isEmpty(serviceGroups)) {
            log.info("******************** Service Groups ************************");
            serviceGroupMap = serviceGroups.stream().peek(serviceGroup -> log.info("{}", serviceGroup))
                                            .collect(Collectors.toMap(ServiceGroup::getId, Function.identity()));
        } else {
            serviceMap = new HashMap<>();
            log.warn("No Service Groups Configured");
        }
        log.info("************************************************************");
    }
    
    /**
     * Get the Service by id and version.
     * @param id
     * @param version
     * @return Service
     */
    public Service getServiceById(final String id, final String version) {
        return serviceMap.get(id);
    }
    
    /**
     * Get the Service by id and version.
     * @param id
     * @param version
     * @return Service
     */
    public ServiceGroup getServiceGroupById(final String id, final String version) {
        return serviceGroupMap.get(id);
    }
    
}