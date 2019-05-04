package com.naqiran.thalam.configuration;

import lombok.Data;

@Data
public class CanaryResource {
    //This could hold the either service name or the discovery id based on the template configuration
    private String id;
    private double weight;
}
