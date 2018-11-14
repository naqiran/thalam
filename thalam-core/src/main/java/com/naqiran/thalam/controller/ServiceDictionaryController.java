package com.naqiran.thalam.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceDictionary;
import com.naqiran.thalam.service.model.ServiceMessage;
import com.naqiran.thalam.service.model.ServiceResponse;

@RestController
@RequestMapping("${aggregator.context.path}/dictionary")
public class ServiceDictionaryController {

    @Autowired
    private ServiceDictionary dictionary;
    
    @GetMapping
    public Map<String,Service> getDictionary() {
        return dictionary.getServiceMap();
    }

    @ExceptionHandler(Exception.class)
    public ServiceResponse errorResponse(final Exception exception) {
        ServiceResponse response = new ServiceResponse();
        ServiceMessage message = new ServiceMessage();
        exception.printStackTrace();
        message.setMessage(exception.getMessage());
        response.addMessage(message);
        return response;
    }

}