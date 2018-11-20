package com.naqiran.thalam;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 
 * @author Nakkeeran Annamalai
 */
@ComponentScan(basePackages = {"com.naqiran.thalam"})
public class ThalamApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThalamApplication.class, args);
    }
}