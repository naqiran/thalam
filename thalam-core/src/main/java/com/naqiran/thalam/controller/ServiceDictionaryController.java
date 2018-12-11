package com.naqiran.thalam.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.naqiran.thalam.configuration.BaseService;
import com.naqiran.thalam.configuration.ServiceDictionary;

/**
 * Dictionary End points for Thalam (Platform) 
 * @author Nakkeeran Annamalai
 *
 */
@RestController
@RequestMapping("${aggregator.context.path}/manage/dictionary")
public class ServiceDictionaryController {

    @Autowired
    private ServiceDictionary dictionary;
    
    @GetMapping
    public Map<String,BaseService> getDictionary() {
        return dictionary.getServiceMap();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getService(final @PathVariable(name = "id") String id) {
        BaseService service = dictionary.getServiceMap().get(id);
        if (service != null) {
            return ResponseEntity.ok().body(service);
        }
        return ResponseEntity.notFound().build(); 
    }
}