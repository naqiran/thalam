package com.naqiran.thalam.configuration;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Data;

@Data
public class AggregatorCoreAutoConfiguration {
    
    @Autowired
    private AggregatorCoreConfiguration coreConfiguration;
}
