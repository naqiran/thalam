package com.naqiran.thalam.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.naqiran.thalam.cache.AggregatorCacheService;
import com.naqiran.thalam.service.model.CacheWrapper;

@RestController
@RequestMapping("${aggregator.context.path}" + "/manage/cache")
public class CacheController {
    
    @Autowired
    private AggregatorCacheService cacheService;
    
    /**
     * 
     * @param cacheName
     * @param cacheKey
     * @return Map
     */
    @ResponseBody
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public ResponseEntity<Object> getValue(final @RequestParam(value = "cacheName", required = true) String cacheName, 
                                    final@RequestParam(value = "cacheKey", required = true) String cacheKey) {
        final CacheWrapper cachedObject = cacheService.getValueFromCache(cacheKey, cacheName);
        final Map<String,Object> response = new HashMap<>();
        response.put("cacheName", cacheName);
        response.put("cacheKey", cacheKey);
        response.put("value", cachedObject);
        return ResponseEntity.ok(response);
    }
}
