package com.enterprise.engine.core.component;

import org.apache.camel.Exchange;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component("overlayEnricher")
public class OverlayDataEnricher {

    private final CacheManager cacheManager;

    public OverlayDataEnricher(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void enrichFromCache(Exchange exchange) {
        // 1. Get the lookup key defined by the AI in the Camel header
        String lookupKey = exchange.getIn().getHeader("OverlayLookupKey", String.class);
        
        if (lookupKey == null) {
            return; // No enrichment requested for this route
        }

        // 2. Fetch data from the local Caffeine cache (sub-millisecond)
        // Note: In reality, a separate async process populates this cache
        Object overlayData = cacheManager.getCache("overlayData").get(lookupKey, Object.class);

        // 3. Store the fetched data in an Exchange Property (NOT the body)
        // We keep the original Kafka event safe in the body.
        exchange.setProperty("EnrichedOverlayData", overlayData != null ? overlayData : Map.of());
    }
}