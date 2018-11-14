package com.naqiran.thalam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 
 * @author Nakkeeran Annamalai
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.naqiran.thalam"})
public class ThalamApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThalamApplication.class, args);
    }
}