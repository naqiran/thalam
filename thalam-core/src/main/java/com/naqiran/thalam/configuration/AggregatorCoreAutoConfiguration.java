package com.naqiran.thalam.configuration;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Data;

/**
 * Auto Configurer for Thalam (Platform)
 * @author Nakkeeran Annamalai
 *
 */
@Data
public class AggregatorCoreAutoConfiguration {
    
    @Autowired
    private AggregatorCoreConfiguration coreConfiguration;
}
