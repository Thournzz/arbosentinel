package com.arbosentinel.yellow;

// ================================================
// YELLOW layer — Caffeine cache configuration
// Cache names power @Cacheable in PINK layer
// TTL and size set in application.properties:
//   spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=300s
// ================================================

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.caffeine.spec:maximumSize=500,expireAfterWrite=300s}")
    private String caffeineSpec;

    // Named caches match @Cacheable(value = "...") annotations in PINK layer
    public static final String DISEASES          = "diseases";
    public static final String DISEASE_DETAIL    = "diseaseDetail";
    public static final String DENGUE_ANNUAL     = "dengueAnnual";
    public static final String DENGUE_SEASONAL   = "dengueSeasonal";
    public static final String MALARIA_BURDEN    = "malariaBurden";
    public static final String MALARIA_REGION    = "malariaRegion";
    public static final String WEST_NILE_TREND   = "westNileTrend";
    public static final String ZIKA_LOCATIONS    = "zikaLocations";
    public static final String SINAN_COUNTS      = "sinanCounts";
    public static final String RISK_SCORES       = "riskScores";
    public static final String PROG_ALERTS       = "progAlerts";
    public static final String DASHBOARD_STATS   = "dashboardStats";
    public static final String PHARMACOLOGY      = "pharmacology";
    public static final String VECTORS           = "vectors";
    public static final String PAHO_CARIBBEAN   = "pahoCaribbean";   // PAHO Caribbean surveillance data

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
            DISEASES, DISEASE_DETAIL, DENGUE_ANNUAL, DENGUE_SEASONAL,
            MALARIA_BURDEN, MALARIA_REGION, WEST_NILE_TREND,
            ZIKA_LOCATIONS, SINAN_COUNTS, RISK_SCORES, PROG_ALERTS,
            DASHBOARD_STATS, PHARMACOLOGY, VECTORS, PAHO_CARIBBEAN
        );
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(300, TimeUnit.SECONDS)
            .recordStats()
        );
        return manager;
    }
}
