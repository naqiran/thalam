package com.naqiran.thalam.configuration;

import lombok.Data;

import java.util.List;

@Data
public class CanaryTemplate {
    private String id;
    private TemplateType type;
    private List<CanaryResource> resources;

    enum TemplateType {
        SERVICE, DISCOVERY, CACHE;
    }
}
