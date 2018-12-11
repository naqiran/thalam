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
import org.springframework.lang.Nullable;
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
    private Map<String,BaseService> serviceMap;
    private Map<String,ServiceGroup> serviceGroupMap;
    
    @PostConstruct
    private void buildServices() {
        log.info("Initializing the Service Dictionary {}", name);
        serviceMap = new HashMap<String,BaseService>();
        if (!CollectionUtils.isEmpty(services)) {
            log.info("******************** Services ******************************");
            serviceMap.putAll(services.stream().peek(service -> log.info("{}", service))
                                            .collect(Collectors.toMap(Service::getId, Function.identity())));
            
        } 
        if (!CollectionUtils.isEmpty(serviceGroups)) {
            log.info("******************** Service Groups ************************");
            serviceMap.putAll(serviceGroups.stream().peek(serviceGroup -> log.info("{}", serviceGroup))
                                            .collect(Collectors.toMap(ServiceGroup::getId, Function.identity())));
        } 
        log.info("************************************************************");
    }
    
    

    /**
     * Get the Service by id and version.
     * @param id
     * @param version
     * @return Service
     */
    public @Nullable BaseService getServiceById(final String id, final String version) {
        return serviceMap.get(id);
    }
}