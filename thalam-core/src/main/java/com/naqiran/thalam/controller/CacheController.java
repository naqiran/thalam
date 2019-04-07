package com.naqiran.thalam.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @GetMapping
    @ResponseBody
    public ResponseEntity<Object> getValue(final @RequestParam(value = "cacheName", required = true) String cacheName, 
                                    final@RequestParam(value = "cacheKey", required = true) String cacheKey) {
        final CacheWrapper cachedObject = cacheService.getValueFromCache(cacheKey, cacheName);
        final Map<String,Object> response = new HashMap<>();
        response.put("cacheName", cacheName);
        response.put("cacheKey", cacheKey);
        response.put("value", cachedObject);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 
     * @param cacheName
     * @param cacheKey
     * @return Map
     */
    @DeleteMapping
    @ResponseBody
    public ResponseEntity<Object> deleteValue(final @RequestParam(value = "cacheName", required = true) String cacheName, 
                                    final@RequestParam(value = "cacheKey", required = true) String cacheKey) {
        cacheService.evictCacheByKey(cacheKey, cacheName);
        final Map<String,Object> response = new HashMap<>();
        response.put("cacheName", cacheName);
        response.put("cacheKey", cacheKey);
        response.put("message", "Delete the Record");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 
     * @param cacheName
     * @param cacheKey
     * @return Map
     */
    @PutMapping
    @ResponseBody
    public ResponseEntity<Object> putValue(final @RequestParam(value = "cacheName", required = true) String cacheName, 
                                    final@RequestParam(value = "cacheKey", required = true) String cacheKey, final @RequestBody CacheWrapper wrapper) {
        wrapper.setSource("Put Request");
        wrapper.setCachedTime(Instant.now());
        cacheService.putValueinCache(cacheKey, cacheName, wrapper);
        final Map<String,Object> response = new HashMap<>();
        response.put("cacheName", cacheName);
        response.put("cacheKey", cacheKey);
        response.put("message", "Added the record in the Cache");
        return ResponseEntity.ok(response);
    }
}
