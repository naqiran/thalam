package com.naqiran.thalam.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

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
    private List<Service> services;
    private Map<String,Service> serviceMap;
    
    @PostConstruct
    private void buildServices() {
        serviceMap = new HashMap<>();
        log.info("Initializing the Service Dictionary {}", name);
        log.info("******************** Intializing Service Dictionary ********************");
        if (!CollectionUtils.isEmpty(services)) {
            services.stream().forEach(service -> {
                log.info("{}", service);
                serviceMap.put(service.getId(), service); 
            });
        } else {
            log.error("No Services Configured");
        }
        log.info("******************** Initialized Service Dictionary ********************");
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
}