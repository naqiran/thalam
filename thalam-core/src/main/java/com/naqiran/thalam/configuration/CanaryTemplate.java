package com.naqiran.thalam.configuration;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class CanaryTemplate {
    @NotBlank
    private String id;
    private TemplateType type;
    private List<CanaryResource> resources;

    enum TemplateType {
        SERVICE, DISCOVERY, CACHE;
    }
}
