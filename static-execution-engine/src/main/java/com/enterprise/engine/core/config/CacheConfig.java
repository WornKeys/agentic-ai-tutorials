package com.enterprise.engine.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String OVERLAY_DATA_CACHE = "overlayData";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(OVERLAY_DATA_CACHE);
        
        // Configure strict boundaries to prevent memory leaks in the engine
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES) // TTL for overlay data
                .maximumSize(10_000)                    // Max entries per pod
                .recordStats());                        // Essential for Actuator metrics
                
        return cacheManager;
    }
}