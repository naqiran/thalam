package com.naqiran.thalam.controller;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.naqiran.thalam.configuration.AggregatorCoreConfiguration;
import com.naqiran.thalam.configuration.BaseService;
import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceDictionary;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;

@RestController
public class SwaggerController {
    
    @Autowired
    private AggregatorCoreConfiguration configuration;
    
    @Autowired
    private ServiceDictionary serviceDictionary;
    
    @ResponseBody
    @GetMapping(value = "${aggregator.context.path}/swagger.json")
    public Swagger getSwagger() {
        Swagger swagger = new Swagger();
        addPaths(swagger);
        swagger.addConsumes(MediaType.APPLICATION_JSON_VALUE);
        swagger.addProduces(MediaType.APPLICATION_JSON_VALUE);
        return swagger;
    }
    
    public void addPaths(final Swagger swagger) {
        final Map<String,BaseService> services = serviceDictionary.getServiceMap();
        for (final Entry<String, BaseService> serviceEntry : services.entrySet()) {
            final BaseService service = serviceEntry.getValue();
            
            if (service instanceof Service) {
                //swagger.model(clazz.getName(), model);
            }
            final Path path = new Path();
            Operation operation = new Operation();
            path.set(RequestMethod.GET.name(), operation);
            swagger.path(getBasePath(service), path);
        }
    }
    
    private String getBasePath(final BaseService service) {
        if (configuration.getContext() != null) {
            String path = configuration.getContext().getPath();
            return path + "/" + service.getId();
        }
        return StringUtils.EMPTY;
    }
}
