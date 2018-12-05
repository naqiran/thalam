package com.naqiran.thalam.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naqiran.thalam.configuration.Service;
import com.naqiran.thalam.configuration.ServiceDictionary;
import com.naqiran.thalam.configuration.ServiceGroup;

/**
 * Dictionary End points for Thalam (Platform) 
 * @author Nakkeeran Annamalai
 *
 */
@RestController
@RequestMapping("${aggregator.context.path}/dictionary")
public class ServiceDictionaryController {

    @Autowired
    private ServiceDictionary dictionary;
    
    @GetMapping
    public Map<String,Service> getDictionary() {
        return dictionary.getServiceMap();
    }
    
    @GetMapping("/service/{id}")
    public ResponseEntity<?> getService(final String id) {
        ServiceGroup serviceGroup = dictionary.getServiceGroupMap().get(id);
        if (serviceGroup != null) {
            return ResponseEntity.ok().body(serviceGroup);
        } else {
            Service service = dictionary.getServiceMap().get(id);
            if (service != null) {
                return ResponseEntity.ok().body(serviceGroup);
            }
        }
        return ResponseEntity.notFound().build(); 
    }
}