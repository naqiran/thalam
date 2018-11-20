package com.naqiran.thalam.sample.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.naqiran.thalam.annotations.EnableServiceAggregator;

/**
 * 
 * @author Nakkeeran Annamalai
 */
@EnableServiceAggregator
@EnableAutoConfiguration
@SpringBootApplication
public class ThalamSampleApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ThalamSampleApplication.class, args);
    }
}